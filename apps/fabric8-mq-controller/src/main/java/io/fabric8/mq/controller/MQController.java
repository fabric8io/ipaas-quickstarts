/*
 *
 *  * Copyright 2005-2015 Red Hat, Inc.
 *  * Red Hat licenses this file to you under the Apache License, version
 *  * 2.0 (the "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  * implied.  See the License for the specific language governing
 *  * permissions and limitations under the License.
 *
 */
package io.fabric8.mq.controller;

import io.fabric8.gateway.SocketWrapper;
import io.fabric8.gateway.handlers.detecting.FutureHandler;
import io.fabric8.gateway.handlers.detecting.Protocol;
import io.fabric8.gateway.handlers.detecting.protocol.mqtt.MqttProtocol;
import io.fabric8.gateway.handlers.detecting.protocol.openwire.OpenwireProtocol;
import io.fabric8.gateway.handlers.detecting.protocol.ssl.SslConfig;
import io.fabric8.gateway.handlers.detecting.protocol.ssl.SslSocketWrapper;
import io.fabric8.gateway.handlers.loadbalancer.ConnectionParameters;
import io.fabric8.mq.controller.camel.CamelRouter;
import io.fabric8.mq.controller.camel.DefaultMessageRouter;
import io.fabric8.mq.controller.multiplexer.MultiplexerController;
import io.fabric8.mq.controller.protocol.ProtocolTransport;
import io.fabric8.mq.controller.protocol.ProtocolTransportFactory;
import io.fabric8.mq.controller.protocol.mqtt.MQTTTransportFactory;
import io.fabric8.mq.controller.protocol.openwire.OpenWireTransportFactory;
import io.fabric8.mq.controller.util.ProtocolMapping;
import io.fabric8.mq.controller.util.Utils;
import io.fabric8.utils.JMXUtils;
import io.fabric8.utils.ShutdownTracker;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.jboss.netty.util.internal.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.core.streams.Pump;
import org.vertx.java.core.streams.ReadStream;

import javax.management.ObjectName;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A MQ Controller which listens on a port and snoops the initial request bytes from a client
 * to detect the protocol and protocol specific connection parameters such a requested
 * virtual host to handle proxying the connection to an appropriate service.
 */
public class MQController extends ServiceSupport implements MQControllerMBean, Handler<Transport> {
    private static final transient Logger LOG = LoggerFactory.getLogger(MQController.class);
    private final String DEFAULT_DOMAIN = "io.fabric8";
    private final String TYPE = MQController.class.getName();
    private final Vertx vertx;
    private final List<NetServer> servers;
    private final BrokerStateInfo brokerStateInfo;
    private final List<MultiplexerController> multiplexerControllers;
    private final AtomicLong receivedConnectionAttempts;
    private final AtomicLong successfulConnectionAttempts;
    private final AtomicLong failedConnectionAttempts;
    private final HashSet<SocketWrapper> socketsConnecting;
    private final HashSet<ConnectedSocketInfo> socketsConnected;
    private final ShutdownTracker shutdownTacker;
    private final AsyncExecutors asyncExecutors;
    private final CamelRouter camelRouter;
    private final FutureHandler<AsyncResult<NetServer>> listenFuture;
    SSLContext sslContext;
    SslSocketWrapper.ClientAuth clientAuth = SslSocketWrapper.ClientAuth.WANT;
    private List<Protocol> protocols;
    private String defaultVirtualHost;
    private int maxProtocolIdentificationLength;
    private SslConfig sslConfig;
    private long connectionTimeout;
    private int port;
    private String host;
    private String name;
    private int numberOfServers;
    private int numberOfMultiplexers;
    private String defaultURI;
    private Map<Object, ObjectName> objectNameMap;
    private ObjectName rootObjectName;

    public MQController() {
        vertx = VertxFactory.newVertx();
        servers = new CopyOnWriteArrayList<>();
        brokerStateInfo = new BrokerStateInfo(this);
        multiplexerControllers = new CopyOnWriteArrayList<>();
        receivedConnectionAttempts = new AtomicLong();
        successfulConnectionAttempts = new AtomicLong();
        failedConnectionAttempts = new AtomicLong();
        socketsConnecting = new HashSet<>();
        socketsConnected = new HashSet<>();
        protocols = new CopyOnWriteArrayList<>();
        shutdownTacker = new ShutdownTracker();
        asyncExecutors = new AsyncExecutors();
        camelRouter = new CamelRouter();
        numberOfServers = Runtime.getRuntime().availableProcessors();
        numberOfMultiplexers = numberOfServers;
        defaultVirtualHost = "broker";
        connectionTimeout = 5000;
        name = "MQController";
        defaultURI = "tcp://localhost:61617";
        objectNameMap = new ConcurrentHashMap<>();
        listenFuture = new FutureHandler<AsyncResult<NetServer>>() {
            @Override
            public void handle(AsyncResult<NetServer> event) {
                if (event.succeeded()) {
                    LOG.info(String.format("MQController listening on %s:%d for protocols: %s", event.result().host(), event.result().port(), getProtocolNames()));
                }
                super.handle(event);
            }
        };
    }

    public void registerInJmx(ObjectName objectName, Object object) throws Exception {
        JMXUtils.registerMBean(object, objectName);
        objectNameMap.put(object, objectName);
    }

    public void unregisterInJmx(Object object) {
        ObjectName objectName = objectNameMap.remove(object);
        if (objectName != null) {
            JMXUtils.unregisterMBean(objectName);
        }
    }

    public ObjectName getRootObjectName() {
        return rootObjectName;
    }

    @Override
    public String toString() {
        return "MQController{" +
                   ", port=" + port +
                   ", host='" + host + '\'' +
                   ", protocols='" + getProtocolNames() + '\'' +
                   '}';
    }

    @Override
    protected void doStop(ServiceStopper stopper) throws Exception {
        for (Object object : objectNameMap.keySet()) {
            unregisterInJmx(object);
        }
        stopper.stop(asyncExecutors);
        close();
    }

    @Override
    protected void doStart() throws Exception {
        validateSettings();
        rootObjectName = Utils.getObjectName(DEFAULT_DOMAIN, "type=" + TYPE);
        registerInJmx(rootObjectName, this);
        //load protocols
        protocols.add(new MqttProtocol());
        protocols.add(new OpenwireProtocol());

        for (Protocol protocol : protocols) {
            maxProtocolIdentificationLength = Math.max(protocol.getMaxIdentificationLength(), maxProtocolIdentificationLength);
        }

        asyncExecutors.start();
        for (int i = 0; i < getNumberOfServers(); i++) {
            NetServer server = vertx.createNetServer().connectHandler(new MQControllerNetSocketHandler(this));
            if (host != null) {
                server = server.listen(port, host, listenFuture);
            } else {
                server = server.listen(port, listenFuture);
            }
            servers.add(server);
        }

        brokerStateInfo.start();
        for (int i = 0; i < getNumberOfMultiplexers(); i++) {
            MultiplexerController multiplexerController = new MultiplexerController(getName() + "-MultiplexController-" + i, brokerStateInfo);
            multiplexerController.start();
            multiplexerControllers.add(multiplexerController);

        }
        camelRouter.start();
    }

    public void close() {
        try {
            camelRouter.stop();
            for (MultiplexerController multiplexerController : multiplexerControllers) {
                multiplexerController.stop();
            }
            for (NetServer server : servers) {
                server.close();
            }
            servers.clear();
            for (SocketWrapper socket : new ArrayList<>(socketsConnecting)) {
                handleConnectFailure(socket, null);
            }
            for (ConnectedSocketInfo socket : new ArrayList<>(socketsConnected)) {
                handleShutdown(socket);
            }
        } catch (Throwable e) {
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBoundPort() throws Exception {
        return FutureHandler.result(listenFuture).port();
    }

    public String getDefaultVirtualHost() {
        return defaultVirtualHost;
    }

    public void setDefaultVirtualHost(String defaultVirtualHost) {
        this.defaultVirtualHost = defaultVirtualHost;
    }

    public CamelRouter getCamelRouter() {
        return camelRouter;
    }

    public Collection<String> getProtocolNames() {
        ArrayList<String> rc = new ArrayList<String>(protocols.size());
        for (Protocol protocol : protocols) {
            rc.add(protocol.getProtocolName());
        }
        return rc;
    }

    public int getNumberOfServers() {
        return numberOfServers;
    }

    public void setNumberOfServers(int numberOfServers) {
        this.numberOfServers = numberOfServers;
    }

    public int getNumberOfMultiplexers() {
        return numberOfMultiplexers;
    }

    public void setNumberOfMultiplexers(int numberOfMultiplexers) {
        this.numberOfMultiplexers = numberOfMultiplexers;
    }

    public String getCamelRoutes() {
        return camelRouter.getCamelRoutes();
    }

    public void setCamelRoutes(String camelRoutes) {
        camelRouter.setCamelRoutes(camelRoutes);
    }

    public ScheduledFuture scheduleAtFixedRate(Runnable runnable, long period, long maxTimeInCall) {
        return asyncExecutors.scheduleAtFixedRate(runnable, period, maxTimeInCall);
    }

    public void execute(final Runnable runnable) {
        asyncExecutors.execute(runnable);
    }

    public AsyncExecutors getAsyncExecutors() {
        return asyncExecutors;
    }

    public void handle(final SocketWrapper socket) {
        shutdownTacker.retain();
        receivedConnectionAttempts.incrementAndGet();
        socketsConnecting.add(socket);

        if (connectionTimeout > 0) {
            vertx.setTimer(connectionTimeout, new Handler<Long>() {
                public void handle(Long timerID) {
                    if (socketsConnecting.contains(socket)) {
                        handleConnectFailure(socket, String.format("MQController client '%s' protocol detection timeout.", socket.remoteAddress()));
                    }
                }
            });
        }

        ReadStream<ReadStream> readStream = socket.readStream();
        readStream.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable e) {
                handleConnectFailure(socket, String.format("Failed to route MQController client '%s' due to: %s", socket.remoteAddress(), e));
            }
        });
        readStream.endHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                handleConnectFailure(socket, String.format("MQController client '%s' closed the connection before it could be routed.", socket.remoteAddress()));
            }
        });
        readStream.dataHandler(new Handler<Buffer>() {
            Buffer received = new Buffer();

            @Override
            public void handle(Buffer event) {
                received.appendBuffer(event);
                for (final Protocol protocol : protocols) {
                    if (protocol.matches(received)) {
                        if ("ssl".equals(protocol.getProtocolName())) {

                            LOG.info(String.format("SSL Connection from '%s'", socket.remoteAddress()));
                            String disabledCypherSuites = null;
                            String enabledCipherSuites = null;
                            if (sslConfig != null) {
                                disabledCypherSuites = sslConfig.getDisabledCypherSuites();
                                enabledCipherSuites = sslConfig.getEnabledCipherSuites();
                            }
                            if (sslContext == null) {
                                try {
                                    if (sslConfig != null) {
                                        sslContext = SSLContext.getInstance(sslConfig.getProtocol());
                                        sslContext.init(sslConfig.getKeyManagers(), sslConfig.getTrustManagers(), null);
                                    } else {
                                        sslContext = SSLContext.getDefault();
                                    }
                                } catch (Throwable e) {
                                    handleConnectFailure(socket, "Could initialize SSL: " + e);
                                    return;
                                }
                            }

                            // lets wrap it up in a SslSocketWrapper.
                            SslSocketWrapper sslSocketWrapper = new SslSocketWrapper(socket);
                            sslSocketWrapper.putBackHeader(received);
                            sslSocketWrapper.initServer(sslContext, clientAuth, disabledCypherSuites, enabledCipherSuites);
                            MQController.this.handle(sslSocketWrapper);
                            return;

                        } else {
                            protocol.snoopConnectionParameters(socket, received, new Handler<ConnectionParameters>() {
                                @Override
                                public void handle(ConnectionParameters connectionParameters) {
                                    // this will install a new dataHandler on the socket.
                                    if (connectionParameters.protocol == null)
                                        connectionParameters.protocol = protocol.getProtocolName();
                                    if (connectionParameters.protocolSchemes == null)
                                        connectionParameters.protocolSchemes = protocol.getProtocolSchemes();
                                    route(socket, connectionParameters, received);
                                }
                            });
                            return;
                        }
                    }
                }
                if (received.length() >= maxProtocolIdentificationLength) {
                    handleConnectFailure(socket, "Connection did not use one of the enabled protocols " + getProtocolNames());
                }
            }
        });
    }

    private void handleConnectFailure(SocketWrapper socket, String reason) {
        if (socketsConnecting.remove(socket)) {
            if (reason != null) {
                LOG.info(reason);
            }
            failedConnectionAttempts.incrementAndGet();
            socket.close();
            shutdownTacker.release();
        }
    }

    public void route(final SocketWrapper socket, ConnectionParameters params, final Buffer received) {
        if (params.protocolVirtualHost == null) {
            params.protocolVirtualHost = defaultVirtualHost;
        }
        ProtocolMapping protocolMapping = createProtocolMapping(params);
        try {
            LOG.info(String.format("Connecting client from '%s' requesting virtual host '%s' to '%s' using the %s protocol",
                                      socket.remoteAddress(), params.protocolVirtualHost, protocolMapping, params.protocol));
            createProtocolClient(params, socket, protocolMapping, received);
        } catch (Throwable e) {
            LOG.warn("Failed to create route for " + params, e);
            handleConnectFailure(socket, String.format("No endpoint available for virtual host '%s' and protocol %s", params.protocolVirtualHost, params.protocol));
        }
    }

    private void createProtocolClient(final ConnectionParameters params, final SocketWrapper socketFromClient, ProtocolMapping protocolMapping, final Buffer received) throws Exception {
        ProtocolTransport transport = getTransport(protocolMapping);
        if (transport != null) {
            successfulConnectionAttempts.incrementAndGet();
            socketsConnecting.remove(socketFromClient);

            final ConnectedSocketInfo connectedInfo = new ConnectedSocketInfo();
            connectedInfo.setParams(params);
            connectedInfo.setProtocolMapping(protocolMapping);
            connectedInfo.setFrom(socketFromClient);
            connectedInfo.setTo(transport);

            Handler<Void> endHandler = new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    handleShutdown(connectedInfo);
                }
            };
            Handler<Throwable> exceptionHandler = new Handler<Throwable>() {
                @Override
                public void handle(Throwable event) {
                    event.printStackTrace();
                    handleShutdown(connectedInfo);
                }
            };

            socketFromClient.readStream().endHandler(endHandler);
            socketFromClient.readStream().exceptionHandler(exceptionHandler);
            transport.endHandler(endHandler);
            transport.exceptionHandler(exceptionHandler);
            transport.stopHandler(endHandler);

            transport.pause();

            addOutbound(protocolMapping, transport);

            transport.write(received);

            Pump writePump = Pump.createPump(transport, socketFromClient.writeStream());
            Pump readPump = Pump.createPump(socketFromClient.readStream(), transport);
            connectedInfo.setWritePump(writePump);
            connectedInfo.setReadPump(readPump);
            socketsConnected.add(connectedInfo);

            writePump.start();
            readPump.start();

            transport.resume();
        } else {
            throw new IOException("Protocol " + protocolMapping.getProtocol() + " is not supported");
        }

    }

    public void handle(Transport transport) {
        handleShutdown(transport);
    }

    public void handleShutdown(Transport transport) {
        if (transport != null) {
            for (ConnectedSocketInfo info : socketsConnected) {
                if (info.to != null && info.to.equals(transport)) {
                    handleShutdown(info);
                    break;
                }
            }
        }
    }

    private ProtocolMapping createProtocolMapping(ConnectionParameters parameters) {
        ProtocolMapping protocolMapping = new ProtocolMapping();
        protocolMapping.setProtocol(parameters.protocol);
        return protocolMapping;
    }

    private void handleShutdown(ConnectedSocketInfo connectedInfo) {
        if (socketsConnected.remove(connectedInfo)) {
            try {
                for (MultiplexerController multiplexerController : multiplexerControllers) {
                    multiplexerController.removeTransport(connectedInfo.to);
                }
                handle(connectedInfo.to);
                connectedInfo.close();
            } catch (Throwable e) {
            } finally {
                shutdownTacker.release();
            }
        }
    }

    public SslConfig getSslConfig() {
        return sslConfig;
    }

    public void setSslConfig(SslConfig sslConfig) {
        this.sslConfig = sslConfig;
    }

    public void runOnContext(Handler<Void> handler) {
        vertx.runOnContext(handler);
    }

    public long getReceivedConnectionAttempts() {
        return receivedConnectionAttempts.get();
    }

    public long getSuccessfulConnectionAttempts() {
        return successfulConnectionAttempts.get();
    }

    public long getFailedConnectionAttempts() {
        return failedConnectionAttempts.get();
    }

    public String[] getConnectingClients() {
        ArrayList<String> rc = new ArrayList<>();
        for (SocketWrapper socket : socketsConnecting) {
            rc.add(socket.remoteAddress().toString());
        }
        return rc.toArray(new String[rc.size()]);
    }

    public String[] getConnectedClients() {
        ArrayList<String> rc = new ArrayList<>();
        for (ConnectedSocketInfo info : socketsConnected) {
            rc.add(info.from.remoteAddress().toString());
        }
        return rc.toArray(new String[rc.size()]);
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDefaultURI() {
        return defaultURI;
    }

    public void setDefaultURI(String defaultURI) {
        this.defaultURI = defaultURI;
    }

    protected void addOutbound(ProtocolMapping protocolMapping, ProtocolTransport transport) throws Exception {
        URI uri = new URI(getDefaultURI());
        //add in Camel Interceptor
        DefaultMessageRouter messageRouter = new DefaultMessageRouter(transport);
        //round robin
        synchronized (multiplexerControllers) {
            if (!multiplexerControllers.isEmpty()) {
                MultiplexerController multiplexerController = multiplexerControllers.remove(0);
                multiplexerController.addTransport(uri, transport);
                multiplexerControllers.add(multiplexerController);
            }
        }
    }

    protected ProtocolTransport getTransport(ProtocolMapping protocolMapping) throws IOException {
        String protocol = protocolMapping.getProtocol().trim();
        ProtocolTransportFactory factory;
        if (protocol.equalsIgnoreCase("mqtt")) {
            factory = new MQTTTransportFactory();
        } else {
            factory = new OpenWireTransportFactory();
        }
        return factory.connect(this, protocolMapping.toString());

    }

    private void validateSettings() {
        if (getNumberOfServers() < 1) {
            LOG.warn("Number of Servers must be 1 or greater - setting to 1");
            setNumberOfServers(1);
        }
        if (getNumberOfMultiplexers() < 1) {
            LOG.warn("Number of Multiplexers must be 1 or greater - setting to 1");
            setNumberOfMultiplexers(1);
        }
    }

    private static class ConnectedSocketInfo {

        private ConnectionParameters params;
        private ProtocolMapping protocolMapping;
        private SocketWrapper from;
        private ProtocolTransport to;
        private Pump readPump;
        private Pump writePump;

        ConnectedSocketInfo() {
        }

        Pump getWritePump() {
            return writePump;
        }

        void setWritePump(Pump writePump) {
            this.writePump = writePump;
        }

        ConnectionParameters getParams() {
            return params;
        }

        void setParams(ConnectionParameters params) {
            this.params = params;
        }

        ProtocolMapping getProtocolMapping() {
            return protocolMapping;
        }

        void setProtocolMapping(ProtocolMapping protocolMapping) {
            this.protocolMapping = protocolMapping;
        }

        SocketWrapper getFrom() {
            return from;
        }

        void setFrom(SocketWrapper from) {
            this.from = from;
        }

        public ProtocolTransport getTo() {
            return to;
        }

        void setTo(ProtocolTransport to) {
            this.to = to;
        }

        Pump getReadPump() {
            return readPump;
        }

        void setReadPump(Pump readPump) {
            this.readPump = readPump;
        }

        void close() throws Exception {
            if (from != null) {
                from.close();
            }
            if (to != null) {
                to.stop();
            }
            if (readPump != null) {
                readPump.stop();
            }
            if (writePump != null) {
                writePump.stop();
            }

        }
    }

    private static class MQControllerNetSocketHandler implements Handler<NetSocket> {
        private final MQController controller;

        public MQControllerNetSocketHandler(MQController controller) {
            this.controller = controller;
        }

        @Override
        public void handle(final NetSocket socket) {
            controller.handle(SocketWrapper.wrap(socket));
        }

    }
}
