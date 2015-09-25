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
import org.apache.activemq.command.KeepAliveInfo;
import org.apache.activemq.transport.InactivityIOException;
import org.apache.activemq.transport.TransportListener;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.apache.activemq.wireformat.WireFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Used to check transports are alive
 */
public class InactivityMonitor extends ServiceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(InactivityMonitor.class);

    protected static long DEFAULT_CHECK_TIME_MILLS = 30000;
    protected static long DEFAULT_MAX_COMPLETION = 2000;
    protected final AtomicBoolean commandReceived = new AtomicBoolean(true);
    protected final AtomicBoolean inReceive = new AtomicBoolean(false);
    protected final AsyncExecutors asyncExecutors;
    protected final ProtocolTransport transport;
    private final AtomicBoolean commandSent = new AtomicBoolean(false);
    private final AtomicBoolean inSend = new AtomicBoolean(false);
    private final boolean enableWriteCheck;
    protected long readCheckTime = DEFAULT_CHECK_TIME_MILLS;
    protected ScheduledFuture readFuture;
    protected WireFormat wireFormat;
    private ScheduledFuture writeFuture;
    private long writeCheckTime = DEFAULT_CHECK_TIME_MILLS;
    private long connectionTimeout = DEFAULT_CHECK_TIME_MILLS;
    private long readGraceTime = DEFAULT_CHECK_TIME_MILLS;
    private long readKeepAliveTime = DEFAULT_CHECK_TIME_MILLS;
    private long initialDelayTime;
    private boolean useKeepAlive;
    private boolean keepAliveResponseRequired;
    private ScheduledFuture connectionFuture;

    public InactivityMonitor(AsyncExecutors asyncExecutors, ProtocolTransport transport, boolean enableWriteCheck) {
        this.asyncExecutors = asyncExecutors;
        this.transport = transport;
        this.enableWriteCheck = enableWriteCheck;
    }

    public void startRead() {
        inReceive.set(true);
    }

    public void finishedRead() {
        inReceive.set(false);
        commandReceived.set(true);
    }

    public long getReadCheckTime() {
        return readCheckTime;
    }

    public void setReadCheckTime(long readCheckTime) {
        this.readCheckTime = readCheckTime;
    }

    public void setUseKeepAlive(boolean val) {
        this.useKeepAlive = val;
    }

    public long getWriteCheckTime() {
        return this.writeCheckTime;
    }

    public void setWriteCheckTime(long writeCheckTime) {
        this.writeCheckTime = writeCheckTime;
    }

    public long getInitialDelayTime() {
        return this.initialDelayTime;
    }

    public void setInitialDelayTime(long initialDelayTime) {
        this.initialDelayTime = initialDelayTime;
    }

    public boolean isKeepAliveResponseRequired() {
        return this.keepAliveResponseRequired;
    }

    public void setKeepAliveResponseRequired(boolean value) {
        this.keepAliveResponseRequired = value;
    }

    protected void doStart() {
        startReadCheck();
        if (enableWriteCheck) {
            startWriteCheck();
        }

    }

    protected void doStop(ServiceStopper stopper) {
        stopWriteCheck();
        stopReadCheck();
        stopConnectionCheck();
    }

    public void startWrite() {
        inSend.set(true);
    }

    public void finishedWrite() {
        inSend.set(false);
        commandSent.set(true);
    }

    protected void startReadCheck() {
        Runnable reader = new Runnable() {
            @Override
            public void run() {
                readCheck();
            }
        };
        readFuture = asyncExecutors.scheduleAtFixedRate(reader, getReadCheckTime(), DEFAULT_MAX_COMPLETION);
    }

    protected void stopReadCheck() {
        ScheduledFuture future = readFuture;
        try {
            if (future != null) {
                future.cancel(true);
            }
        } catch (Throwable e) {
        }
    }

    protected void readCheck() {
        if (!inReceive.get()) {
            if (!isStopping() && !isStopped()) {
                if (!commandReceived.get()) {
                    onException(new IOException("InactivityMonitor - no traffic from " + transport.getRemoteAddress()));
                }
            }
            commandReceived.set(false);
        }
    }

    protected void onException(final Throwable e) {
        asyncExecutors.execute(new Runnable() {
            @Override
            public void run() {
                handleException(e);
            }
        });
    }

    private void handleException(Throwable e) {
        TransportListener listener = transport.getTransportListener();
        if (listener != null) {
            IOException ioException;
            if (e instanceof IOException) {
                ioException = (IOException) e;
            } else {
                ioException = new IOException(e.getMessage());
                ioException.initCause(e);
            }
            listener.onException(ioException);
        }
        try {
            stop();
        } catch (Throwable ex) {
            LOG.debug("Caught an exception in onException", ex);
        }
    }

    public void processKeepAliveReceived(KeepAliveInfo info) {
        if (info.isResponseRequired()) {
            info.setResponseRequired(false);
            try {
                transport.oneway(info);
            } catch (Exception e) {
                LOG.warn("Failed to send KeepAlive", e);
            }
        }
    }

    private void stopWriteCheck() {
        ScheduledFuture future = writeFuture;
        try {
            if (future != null) {
                future.cancel(true);
            }
        } catch (Throwable e) {
        }
    }

    private void startWriteCheck() {
        writeCheckTime = readCheckTime > 3 ? readCheckTime / 3 : readCheckTime;
        Runnable writer = new Runnable() {
            @Override
            public void run() {
                writeCheck();
            }
        };
        writeFuture = asyncExecutors.scheduleAtFixedRate(writer, writeCheckTime, DEFAULT_MAX_COMPLETION);
    }

    private void writeCheck() {
        if (!inSend.get() && !isStopping() && !isStopped()) {
            if (!commandSent.get()) {
                asyncExecutors.execute(new Runnable() {
                    @Override
                    public void run() {
                        KeepAliveInfo info = new KeepAliveInfo();
                        info.setResponseRequired(true);
                        try {
                            transport.oneway(info);
                        } catch (IOException e) {
                            onException(e);
                        }
                    }
                });
            }
        }
        commandSent.set(false);
    }

    public synchronized void startConnectCheck(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        if (connectionTimeout > 0 && connectionFuture == null) {

            long connectionCheckInterval = Math.min(connectionTimeout, 1000);

            Runnable connection = new Runnable() {
                long now = System.currentTimeMillis();

                public void run() {
                    connectionCheck(now);
                }
            };
            connectionFuture = asyncExecutors.scheduleAtFixedRate(connection, connectionCheckInterval, DEFAULT_MAX_COMPLETION);
        }
    }

    public synchronized void stopConnectionCheck() {
        ScheduledFuture future = connectionFuture;
        try {
            if (future != null) {
                future.cancel(true);
            }
        } catch (Throwable e) {
        }
        connectionFuture = null;
    }

    private void connectionCheck(long startTime) {
        long now = System.currentTimeMillis();

        if (!isStopping() && !isStopped()) {
            if ((now - startTime) >= connectionTimeout && connectionFuture != null && !connectionFuture.isCancelled()) {
                LOG.debug("No CONNECT frame received in time for " + InactivityMonitor.this.toString() + "! Throwing InactivityIOException.");

                stopConnectionCheck();
                onException(new InactivityIOException("Channel was inactive for too (>" + (readKeepAliveTime + readGraceTime) + ") long: "
                                                          + transport.getRemoteAddress()));
            }
        }
    }
}
