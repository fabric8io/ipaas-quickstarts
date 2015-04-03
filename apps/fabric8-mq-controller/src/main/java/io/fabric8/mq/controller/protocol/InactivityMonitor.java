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
package io.fabric8.mq.controller.protocol;

import io.fabric8.mq.controller.MQController;
import org.apache.activemq.transport.TransportListener;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Used to check transports are alive
 */
public abstract class InactivityMonitor extends ServiceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(InactivityMonitor.class);

    protected static long DEFAULT_CHECK_TIME_MILLS = 30000;
    protected static long DEFAULT_MAX_COMPLETION = 2000;
    protected final AtomicBoolean commandReceived = new AtomicBoolean(true);
    protected final AtomicBoolean inReceive = new AtomicBoolean(false);
    protected final MQController gateway;
    protected final ProtocolTransport transport;
    protected long readCheckTime = DEFAULT_CHECK_TIME_MILLS;
    protected ScheduledFuture readFuture;

    public InactivityMonitor(MQController gateway, ProtocolTransport transport) {
        this.gateway = gateway;
        this.transport = transport;
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

    protected void doStop(ServiceStopper stopper) {
        stopReadCheck();
    }

    protected void startReadCheck() {
        Runnable reader = new Runnable() {
            @Override
            public void run() {
                readCheck();
            }
        };
        readFuture = gateway.scheduleAtFixedRate(reader, getReadCheckTime(), DEFAULT_MAX_COMPLETION);
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
        gateway.execute(new Runnable() {
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
}
