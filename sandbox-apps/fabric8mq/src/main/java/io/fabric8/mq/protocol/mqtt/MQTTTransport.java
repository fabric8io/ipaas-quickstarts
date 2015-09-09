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

package io.fabric8.mq.protocol.mqtt;

import io.fabric8.mq.AsyncExecutors;
import io.fabric8.mq.protocol.InactivityMonitor;
import io.fabric8.mq.protocol.ProtocolTransport;
import org.apache.activemq.command.Command;
import org.apache.activemq.transport.TransportListener;
import org.apache.activemq.transport.TransportSupport;
import org.apache.activemq.transport.mqtt.MQTTWireFormat;
import org.apache.activemq.util.ServiceStopper;
import org.fusesource.mqtt.codec.MQTTFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Vertx ReadStream/WriteStream and ActiveMQ Transport that just happens to build MQTT commands
 * What could possibly go wrong ??
 */

public class MQTTTransport extends TransportSupport implements ProtocolTransport<MQTTTransport> {
    private static final transient Logger LOG = LoggerFactory.getLogger(MQTTTransport.class);

    private final Vertx vertx;
    private final AtomicInteger receiveCounter = new AtomicInteger();
    private final String name;
    private final MQTTWriteStream writeStream;
    private final MQTTReadStream readStream;
    private final MQTTProtocolConverter protocolConverter;
    private final InactivityMonitor inactivityMonitor;
    private Handler<Throwable> exceptionHandler;
    private long connectAttemptTimeout;
    private Handler<Void> stopHandler;

    protected MQTTTransport(Vertx vertx, AsyncExecutors asyncExecutors, String name, MQTTWireFormat wireFormat) {
        this.name = name;
        this.vertx = vertx;
        this.inactivityMonitor = new InactivityMonitor(asyncExecutors, this, false);
        writeStream = new MQTTWriteStream(this, wireFormat);
        readStream = new MQTTReadStream(this, wireFormat);
        protocolConverter = new MQTTProtocolConverter(this);
    }

    public InactivityMonitor getInactivityMonitor() {
        return inactivityMonitor;
    }

    public MQTTTransport stopHandler(Handler<Void> handler) {
        stopHandler = handler;
        return this;
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        Handler<Void> handler = stopHandler;
        if (handler != null) {
            handler.handle(null);
        }
        serviceStopper.stop(writeStream);
        serviceStopper.stop(readStream);
    }

    @Override
    protected void doStart() throws Exception {
        inactivityMonitor.startConnectCheck(getConnectAttemptTimeout());
        writeStream.start();
        readStream.start();
    }

    public void sendToActiveMQ(Command command) {
        super.doConsume(command);
    }

    protected void doConsumeMQTT(MQTTFrame frame) {
        try {
            protocolConverter.onMQTTCommand(frame);
        } catch (Throwable e) {
            handleException(e);
        }
    }

    public void sendToMQTT(MQTTFrame command) throws IOException {
        readStream.sendToVertx(command);
    }

    @Override
    public synchronized void oneway(Object o) {
        try {
            protocolConverter.onActiveMQCommand((Command) o);
        } catch (Exception e) {
            handleException(e);
        }
    }

    @Override
    public String getRemoteAddress() {
        return name;
    }

    @Override
    public int getReceiveCounter() {
        return receiveCounter.get();
    }

    @Override
    public MQTTTransport write(final Buffer buffer) {
        writeStream.write(buffer);
        return this;
    }

    protected void runOnContext(Handler<Void> handler) {
        vertx.runOnContext(handler);
    }

    public MQTTTransport setWriteQueueMaxSize(int i) {
        writeStream.setWriteQueueMaxSize(i);
        return this;
    }

    @Override
    public boolean writeQueueFull() {
        return writeStream.writeQueueFull();
    }

    @Override
    public MQTTTransport drainHandler(Handler<Void> handler) {
        writeStream.drainHandler(handler);
        return this;
    }

    @Override
    public MQTTTransport endHandler(Handler<Void> handler) {
        readStream.endHandler(handler);
        return this;
    }

    @Override
    public MQTTTransport dataHandler(Handler<Buffer> handler) {
        readStream.dataHandler(handler);
        return this;
    }

    @Override
    public MQTTTransport pause() {
        readStream.pause();
        return this;
    }

    @Override
    public MQTTTransport resume() {
        readStream.resume();
        return this;
    }

    @Override
    public MQTTTransport exceptionHandler(Handler<Throwable> handler) {
        this.exceptionHandler = handler;
        return this;
    }

    public long getConnectAttemptTimeout() {
        return connectAttemptTimeout;
    }

    public void setConnectAttemptTimeout(long connectTimeout) {
        this.connectAttemptTimeout = connectTimeout;
    }

    protected void handleHandlerException(Throwable t) {
        LOG.error("Got an exception in handler ", t);
    }

    protected void handleException(Throwable t) {
        LOG.error("Got an exception ", t);
        if (exceptionHandler != null) {
            try {
                exceptionHandler.handle(t);
            } catch (Throwable t2) {
                handleHandlerException(t2);
            }
        }
        TransportListener l = getTransportListener();
        if (l != null) {
            IOException ioException;
            if (t instanceof IOException) {
                ioException = (IOException) t;
            } else {
                ioException = new IOException(t.getMessage());
                ioException.initCause(t);
            }
            l.onException(ioException);
        }
    }

}
