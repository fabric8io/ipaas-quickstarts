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

package io.fabric8.mq.protocol;

import io.fabric8.mq.AsyncExecutors;
import io.fabric8.mq.model.Model;
import io.fabric8.mq.multiplexer.Multiplexer;
import io.fabric8.mq.util.SocketWrapper;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
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

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestProtocolServer extends ServiceSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(TestProtocolServer.class);
    private final int TIMEOUT = 30;
    private TestMessageDistribution testMessageDistribution = new TestMessageDistribution();
    private Vertx vertx = VertxFactory.newVertx();
    @Inject
    private Model model;
    @Inject
    private AsyncExecutors asyncExecutors;
    private NetServer netServer;
    private ProtocolTransportFactory protocolTransportFactory;
    private Transport outBound;
    private Multiplexer multiplexer;
    private int port = 0;
    private int boundPort;
    private int transportCounter = 0;

    public Vertx getVertx() {
        return vertx;
    }

    public AsyncExecutors getAsyncExecutors() {
        return asyncExecutors;
    }

    public String getBrokerURI() {
        return testMessageDistribution.getBrokerURI();
    }

    public int getBoundPort() {
        return boundPort;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ProtocolTransportFactory getProtocolTransportFactory() {
        return protocolTransportFactory;
    }

    public void setProtocolTransportFactory(ProtocolTransportFactory protocolTransportFactory) {
        this.protocolTransportFactory = protocolTransportFactory;
    }

    public Transport getOutBound() {
        return outBound;
    }

    ProtocolTransport getProtocolTransport() throws IOException {
        return protocolTransportFactory.connect(vertx, asyncExecutors, "testConnection-" + (transportCounter++));
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        if (testMessageDistribution != null) {
            testMessageDistribution.stop();
        }
        if (netServer != null) {
            netServer.close();
        }

        if (asyncExecutors != null) {
            asyncExecutors.stop();
        }
    }

    @Override
    protected void doStart() throws Exception {
        asyncExecutors.start();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        FutureHandler<AsyncResult<NetServer>> listenFuture = new FutureHandler<AsyncResult<NetServer>>() {
            @Override
            public void handle(AsyncResult<NetServer> event) {
                if (event.succeeded()) {
                    if (event.result().port() != 0) {
                        boundPort = event.result().port();
                    }
                    countDownLatch.countDown();
                }
                super.handle(event);
            }
        };

        netServer = vertx.createNetServer().connectHandler(new Handler<NetSocket>() {
            @Override
            public void handle(final NetSocket socket) {
                doHandle(SocketWrapper.wrap(socket));
            }
        });

        netServer.listen(port, listenFuture);
        model.start();
        multiplexer = new Multiplexer(model, "test", asyncExecutors, testMessageDistribution);
        multiplexer.start();

        if (!countDownLatch.await(TIMEOUT, TimeUnit.SECONDS)) {
            throw new IllegalStateException("timed out waiting for bound port");
        }
    }

    private void doHandle(final SocketWrapper socket) {

        ReadStream<ReadStream> readStream = socket.readStream();
        readStream.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable e) {
                String err = String.format("Failed to route client '%s' due to: %s", socket.remoteAddress(), e);
                LOG.error(err, e);
            }
        });
        readStream.endHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                String err = String.format("MQController client '%s' closed the connection before it could be routed.", socket.remoteAddress());
                LOG.warn(err);
            }
        });
        readStream.dataHandler(new Handler<Buffer>() {
            Buffer received = new Buffer();

            @Override
            public void handle(Buffer event) {
                received.appendBuffer(event);
                try {
                    route(socket, received);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void route(final SocketWrapper socketFromClient, final Buffer received) throws Exception {
        ProtocolTransport transport = getProtocolTransport();

        Handler<Void> endHandler = new Handler<Void>() {
            @Override
            public void handle(Void event) {
                try {
                    socketFromClient.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        Handler<Throwable> exceptionHandler = new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                event.printStackTrace();
                try {
                    socketFromClient.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        socketFromClient.readStream().endHandler(endHandler);
        socketFromClient.readStream().exceptionHandler(exceptionHandler);
        transport.endHandler(endHandler);
        transport.exceptionHandler(exceptionHandler);
        transport.stopHandler(endHandler);

        transport.pause();

        addOutbound(transport);

        transport.write(received);

        Pump writePump = Pump.createPump(transport, socketFromClient.writeStream());
        Pump readPump = Pump.createPump(socketFromClient.readStream(), transport);

        writePump.start();
        readPump.start();

        transport.resume();

    }

    private void addOutbound(final ProtocolTransport inbound) throws Exception {
        multiplexer.addInput("test", inbound);
    }
}


