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

package io.fabric8.mq.protocol.openwire;

import io.fabric8.mq.AsyncExecutors;
import io.fabric8.mq.protocol.InactivityMonitor;
import io.fabric8.mq.protocol.ProtocolTransport;
import org.apache.activemq.AsyncCallback;
import org.apache.activemq.command.Command;
import org.apache.activemq.command.KeepAliveInfo;
import org.apache.activemq.command.WireFormatInfo;
import org.apache.activemq.openwire.OpenWireFormat;
import org.apache.activemq.transport.TransportListener;
import org.apache.activemq.transport.TransportSupport;
import org.apache.activemq.util.IOExceptionSupport;
import org.apache.activemq.util.ServiceStopper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;

import javax.jms.JMSException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Vertx ReadStream/WriteStream and ActiveMQ Transport that just happens to build OpenWire commands
 * What could possibly go wrong ??
 */

public class OpenWireTransport extends TransportSupport implements ProtocolTransport<OpenWireTransport> {
    private static final transient Logger LOG = LoggerFactory.getLogger(OpenWireTransport.class);
    private static final long NEGIOTIATE_TIMEOUT = 15000L;
    private final AtomicInteger receiveCounter = new AtomicInteger();
    private final String name;
    private final OpenWireFormat openWireFormat;
    private final OpenWireWriteStream writeStream;
    private final OpenWireReadStream readStream;
    private final Vertx vertx;
    private final InactivityMonitor inactivityMonitor;
    private final AtomicBoolean firstStart;
    private final CountDownLatch readyCountDownLatch;
    private final CountDownLatch wireInfoSentDownLatch;
    private final int minimumVersion;
    private Handler<Throwable> exceptionHandler;
    private Handler<Void> stopHandler;
    private long negotiateTimeout = NEGIOTIATE_TIMEOUT;

    protected OpenWireTransport(Vertx vertx, AsyncExecutors asyncExecutors, String name, OpenWireFormat wireFormat) {
        this.name = name;
        this.vertx = vertx;
        this.openWireFormat = wireFormat;
        writeStream = new OpenWireWriteStream(this, wireFormat);
        readStream = new OpenWireReadStream(this, wireFormat);
        firstStart = new AtomicBoolean(true);
        readyCountDownLatch = new CountDownLatch(1);
        wireInfoSentDownLatch = new CountDownLatch(1);
        minimumVersion = 1;
        inactivityMonitor = new InactivityMonitor(asyncExecutors, this, true);
        try {
            if (wireFormat.getPreferedWireFormatInfo() != null) {
                setNegotiateTimeout(wireFormat.getPreferedWireFormatInfo().getMaxInactivityDurationInitalDelay());
            }
        } catch (IOException ignored) {
        }

    }

    public OpenWireTransport stopHandler(Handler<Void> handler) {
        stopHandler = handler;
        return this;
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        Handler<Void> handler = stopHandler;
        if (handler != null) {
            handler.handle(null);
        }
        readyCountDownLatch.countDown();
        serviceStopper.stop(writeStream);
        serviceStopper.stop(readStream);
        serviceStopper.stop(inactivityMonitor);
    }

    @Override
    protected void doStart() throws Exception {
        writeStream.start();
        readStream.start();
        inactivityMonitor.start();
        if (firstStart.compareAndSet(true, false)) {
            sendWireFormat();
        }
    }

    public void sendWireFormat() throws IOException {

        WireFormatInfo info = openWireFormat.getPreferedWireFormatInfo();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Sending: " + info);
        }
        readStream.sendToVertx(info, new AsyncCallback() {
            @Override
            public void onSuccess() {
                wireInfoSentDownLatch.countDown();
            }

            @Override
            public void onException(JMSException exception) {
                wireInfoSentDownLatch.countDown();
            }
        });
    }

    @Override
    public synchronized void oneway(Object o) throws IOException {
        inactivityMonitor.startWrite();
        try {
            if (!readyCountDownLatch.await(negotiateTimeout, TimeUnit.MILLISECONDS)) {
                throw new IOException("Wire format negotiation timeout: peer did not send his wire format.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException();
        }
        readStream.sendToVertx(o);
        inactivityMonitor.finishedWrite();
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
    public OpenWireTransport write(final Buffer buffer) {
        writeStream.write(buffer);
        return this;
    }

    protected void runOnContext(Handler<Void> handler) {
        vertx.runOnContext(handler);
    }

    public OpenWireTransport setWriteQueueMaxSize(int i) {
        writeStream.setWriteQueueMaxSize(i);
        return this;
    }

    @Override
    public boolean writeQueueFull() {
        return writeStream.writeQueueFull();
    }

    @Override
    public OpenWireTransport drainHandler(Handler<Void> handler) {
        writeStream.drainHandler(handler);
        return this;
    }

    @Override
    public OpenWireTransport endHandler(Handler<Void> handler) {
        readStream.endHandler(handler);
        return this;
    }

    @Override
    public OpenWireTransport dataHandler(Handler<Buffer> handler) {
        readStream.dataHandler(handler);
        return this;
    }

    @Override
    public OpenWireTransport pause() {
        readStream.pause();
        return this;
    }

    @Override
    public OpenWireTransport resume() {
        readStream.resume();
        return this;
    }

    @Override
    public OpenWireTransport exceptionHandler(Handler<Throwable> handler) {
        this.exceptionHandler = handler;
        return this;
    }

    protected void handleHandlerException(Throwable t) {
        LOG.error("Got an exception in handler ", t);
    }

    protected void handleException(Throwable t) {
        LOG.error("Got an exception ", t);
        readyCountDownLatch.countDown();
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

    public void negotiate(WireFormatInfo info) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Received WireFormat: " + info);
        }

        try {
            wireInfoSentDownLatch.await(5, TimeUnit.MINUTES);

            if (LOG.isDebugEnabled()) {
                LOG.debug(this + " before negotiation: " + openWireFormat);
            }
            if (!info.isValid()) {
                onException(new IOException("Remote wire format magic is invalid"));
            } else if (info.getVersion() < minimumVersion) {
                onException(new IOException("Remote wire format (" + info.getVersion() + ") is lower the minimum version required (" + minimumVersion + ")"));
            }

            openWireFormat.renegotiateWireFormat(info);

            if (LOG.isDebugEnabled()) {
                LOG.debug(this + " after negotiation: " + openWireFormat);
            }

        } catch (IOException e) {
            onException(e);
        } catch (InterruptedException e) {
            onException((IOException) new InterruptedIOException().initCause(e));
        } catch (Exception e) {
            onException(IOExceptionSupport.create(e));
        }
        readyCountDownLatch.countDown();
        onWireFormatNegotiated(info);
    }

    public void doConsume(Object o) {
        inactivityMonitor.startRead();
        Command command = (Command) o;
        if (command.isWireFormatInfo()) {
            WireFormatInfo info = (WireFormatInfo) command;
            negotiate(info);
        }
        if (command.getDataStructureType() == KeepAliveInfo.DATA_STRUCTURE_TYPE) {
            KeepAliveInfo keepAliveInfo = (KeepAliveInfo) command;
            inactivityMonitor.processKeepAliveReceived(keepAliveInfo);
        } else {
            super.doConsume(command);
        }
        inactivityMonitor.finishedRead();
    }

    protected void onWireFormatNegotiated(WireFormatInfo info) {
        LOG.debug("Negotiated wireFormat:" + this.openWireFormat);
        if (!isStopping() && !isStopped()) {
            try {
                inactivityMonitor.setReadCheckTime(info.getMaxInactivityDuration());
                inactivityMonitor.start();
            } catch (Exception e) {
                handleException(e);
            }
        }
    }

    public long getNegotiateTimeout() {
        return negotiateTimeout;
    }

    public void setNegotiateTimeout(long negotiateTimeout) {
        this.negotiateTimeout = negotiateTimeout;
    }
}
