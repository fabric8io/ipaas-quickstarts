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
package io.fabric8.mq;

import io.fabric8.mq.interceptors.apiman.APIManRouter;
import io.fabric8.mq.interceptors.camel.DefaultMessageRouter;
import io.fabric8.mq.model.Model;
import io.fabric8.mq.multiplexer.MultiplexerController;
import io.fabric8.mq.protocol.FutureHandler;
import io.fabric8.mq.protocol.ProtocolDetector;
import io.fabric8.mq.protocol.ProtocolTransport;
import io.fabric8.mq.protocol.ProtocolTransportFactory;
import io.fabric8.mq.protocol.mqtt.MQTTTransportFactory;
import io.fabric8.mq.protocol.mqtt.MqttProtocol;
import io.fabric8.mq.protocol.openwire.OpenWireTransportFactory;
import io.fabric8.mq.protocol.openwire.OpenwireProtocol;
import io.fabric8.mq.protocol.ssl.SslConfig;
import io.fabric8.mq.protocol.ssl.SslSocketWrapper;
import io.fabric8.mq.protocol.stomp.StompProtocol;
import io.fabric8.mq.protocol.stomp.StompTransportFactory;
import io.fabric8.mq.util.ConnectedSocketInfo;
import io.fabric8.mq.util.ConnectionParameters;
import io.fabric8.mq.util.ProtocolMapping;
import io.fabric8.mq.util.SocketWrapper;
import io.fabric8.utils.JMXUtils;
import io.fabric8.utils.ShutdownTracker;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.util.ServiceStopper;
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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.management.ObjectName;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * A MQ Controller which listens on a port and snoops the initial request bytes from a client
 * to detect the protocol and protocol specific connection parameters such a requested
 * virtual host to handle proxying the connection to an appropriate service.
 */
@ApplicationScoped
@Default
public class Fabric8MQ extends BrokerStateInfo implements Handler<Transport> {
    private static final transient Logger LOG = LoggerFactory.getLogger(Fabric8MQ.class);
    private final String TYPE = Fabric8MQ.class.getName();
    private final Vertx vertx;
    private final List<NetServer> servers;
    private final List<MultiplexerController> multiplexerControllers;
    private final HashSet<SocketWrapper> socketsConnecting;
    private final HashSet<ConnectedSocketInfo> socketsConnected;
    private final ShutdownTracker shutdownTacker;
    SSLContext sslContext;
    SslSocketWrapper.ClientAuth clientAuth = SslSocketWrapper.ClientAuth.WANT;
    private List<ProtocolDetector> protocolDetectors;
    private int maxProtocolIdentificationLength;
    private SslConfig sslConfig;
    private ObjectName controllerObjectName;
    private int boundPort;
    private String host;

    public Fabric8MQ() {
        vertx = VertxFactory.newVertx();
        servers = new CopyOnWriteArrayList<>();
        multiplexerControllers = new CopyOnWriteArrayList<>();
        socketsConnecting = new HashSet<>();
        socketsConnected = new HashSet<>();
        protocolDetectors = new CopyOnWriteArrayList<>();
        shutdownTacker = new ShutdownTracker();
    }

    @Override
    public String toString() {
        return "Fabric8MQ{" +
                   ", url='" + getFabric8MQStatus().getControllerHost() + ":" + getFabric8MQStatus().getControllerPort() + '\'' +
                   ", protocols='" + getProtocolNames() + '\'' +
                   '}';
    }

    @Override
    protected void doStop(ServiceStopper stopper) throws Exception {
        super.doStop(stopper);
        close();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        //getLoad protocols
        protocolDetectors.add(new MqttProtocol());
        protocolDetectors.add(new OpenwireProtocol());
        protocolDetectors.add(new StompProtocol());

        for (ProtocolDetector protocolDetector : protocolDetectors) {
            maxProtocolIdentificationLength = Math.max(protocolDetector.getMaxIdentificationLength(), maxProtocolIdentificationLength);
        }
        final String hostName = getFabric8MQStatus().getControllerHost();
        int port = getFabric8MQStatus().getControllerPort();

        int numberOfMultiplexers = getFabric8MQStatus().getNumberOfMultiplexers();
        int numberOfServers = getFabric8MQStatus().getNumberOfSevers();
        final CountDownLatch countDownLatch = new CountDownLatch(numberOfServers);

        FutureHandler<AsyncResult<NetServer>> listenFuture = new FutureHandler<AsyncResult<NetServer>>() {
            @Override
            public void handle(AsyncResult<NetServer> event) {
                if (event.succeeded()) {
                    if (event.result().port() != 0) {
                        boundPort = event.result().port();
                        host = event.result().host();
                    }
                    countDownLatch.countDown();
                }
                super.handle(event);
            }
        };

        for (int i = 0; i < getFabric8MQStatus().getNumberOfSevers(); i++) {
            NetServer server = vertx.createNetServer().connectHandler(new MQControllerNetSocketHandler(this));
            if (hostName != null && !hostName.isEmpty()) {
                server = server.listen(port, hostName, listenFuture);
            } else {
                server = server.listen(port, listenFuture);
            }
            servers.add(server);
        }
        countDownLatch.await();

        for (int i = 0; i < numberOfMultiplexers; i++) {
            String name = getFabric8MQStatus().getName();
            MultiplexerController multiplexerController = new MultiplexerController(name
                                                                                        + "-MultiplexController-" + i, this);
            multiplexerController.start();
            multiplexerControllers.add(multiplexerController);
        }
        controllerObjectName = new ObjectName(Model.DEFAULT_JMX_DOMAIN, "name", Fabric8MQ.class.getName());
        JMXUtils.registerMBean(getFabric8MQStatus(), controllerObjectName);
        getFabric8MQStatus().setBoundPort(boundPort);
        getFabric8MQStatus().setHost(host);

        String info = String.format("Successfully launched Fabric8MQ: listening on %s:%d for protocols: %s", host, boundPort, getProtocolNames());
        LOG.info(info);
        System.err.print(info);
    }

    private void close() {
        try {
            if (controllerObjectName != null) {
                JMXUtils.unregisterMBean(controllerObjectName);
            }
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
            LOG.debug("Caught an error in close", e);
        }
    }

    public int getBoundPort() {
        return boundPort;
    }

    public String getHost() {
        return host;
    }

    public Collection<String> getProtocolNames() {
        ArrayList<String> rc = new ArrayList<>(protocolDetectors.size());
        for (ProtocolDetector protocolDetector : protocolDetectors) {
            rc.add(protocolDetector.getProtocolName());
        }
        return rc;
    }

    public void handle(final SocketWrapper socket) {
        Fabric8MQStatus status = getFabric8MQStatus();
        shutdownTacker.retain();
        status.incrementReceivedConnectionAttempts();
        socketsConnecting.add(socket);

        if (status.getConnectionTimeout() > 0) {
            vertx.setTimer(status.getConnectionTimeout(), new Handler<Long>() {
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
                for (final ProtocolDetector protocolDetector : protocolDetectors) {
                    if (protocolDetector.matches(received)) {
                        if ("ssl".equals(protocolDetector.getProtocolName())) {

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
                            Fabric8MQ.this.handle(sslSocketWrapper);
                            return;

                        } else {
                            protocolDetector.snoopConnectionParameters(socket, received, new Handler<ConnectionParameters>() {
                                @Override
                                public void handle(ConnectionParameters connectionParameters) {
                                    // this will install a new dataHandler on the socket.
                                    if (connectionParameters.protocol == null)
                                        connectionParameters.protocol = protocolDetector.getProtocolName();
                                    if (connectionParameters.protocolSchemes == null)
                                        connectionParameters.protocolSchemes = protocolDetector.getProtocolSchemes();
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
            Fabric8MQStatus status = getFabric8MQStatus();
            status.incrementFailedConnectionAttempts();
            socket.close();
            shutdownTacker.release();
        }
    }

    public void route(final SocketWrapper socket, ConnectionParameters params, final Buffer received) {
        if (params.protocolVirtualHost == null) {
            params.protocolVirtualHost = getFabric8MQStatus().getDefaultVirtualHost();
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
            Fabric8MQStatus status = getFabric8MQStatus();
            status.incrementSuccessfulConnectionAttempts();
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
                if (info.getTo() != null && info.getTo().equals(transport)) {
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
                    multiplexerController.removeTransport(connectedInfo.getTo());
                }
                handle(connectedInfo.getTo());
                connectedInfo.close();
            } catch (Throwable e) {
                LOG.debug("Caught an error in handleShutdown", e);
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

    protected void addOutbound(ProtocolMapping protocolMapping, ProtocolTransport transport) throws Exception {
        String fromURI = transport.getRemoteAddress();
        String protocol = protocolMapping.getProtocol();

        //add in Camel Interceptor
        DefaultMessageRouter messageRouter = new DefaultMessageRouter(transport);
        //add in APIMan Interceptor
        APIManRouter apiManRouter = new APIManRouter(messageRouter);
        //round robin
        synchronized (multiplexerControllers) {
            if (!multiplexerControllers.isEmpty()) {
                MultiplexerController multiplexerController = multiplexerControllers.remove(0);
                multiplexerController.addTransport(protocol, apiManRouter);
                multiplexerControllers.add(multiplexerController);
            }
        }
    }

    protected ProtocolTransport getTransport(ProtocolMapping protocolMapping) throws IOException {
        String protocol = protocolMapping.getProtocol().trim();
        ProtocolTransportFactory factory;
        if (protocol.equalsIgnoreCase("mqtt")) {
            factory = new MQTTTransportFactory();
        } else if (protocol.equalsIgnoreCase("stomp")) {
            factory = new StompTransportFactory();
        } else if (protocol.equalsIgnoreCase("amqp")) {
            throw new IOException("AMQP not supported");
        } else {
            factory = new OpenWireTransportFactory();
        }
        return factory.connect(vertx, asyncExecutors, protocolMapping.toString());

    }

    private static class MQControllerNetSocketHandler implements Handler<NetSocket> {
        private final Fabric8MQ controller;

        public MQControllerNetSocketHandler(Fabric8MQ controller) {
            this.controller = controller;
        }

        @Override
        public void handle(final NetSocket socket) {
            controller.handle(SocketWrapper.wrap(socket));
        }

    }
}
