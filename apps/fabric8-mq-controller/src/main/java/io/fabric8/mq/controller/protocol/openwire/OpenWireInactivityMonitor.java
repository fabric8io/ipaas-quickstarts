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
package io.fabric8.mq.controller.protocol.openwire;

import io.fabric8.mq.controller.AsyncExecutors;
import io.fabric8.mq.controller.protocol.InactivityMonitor;
import io.fabric8.mq.controller.protocol.ProtocolTransport;
import org.apache.activemq.command.KeepAliveInfo;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.wireformat.WireFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Used to check transports are alive
 */
public class OpenWireInactivityMonitor extends InactivityMonitor {

    private static final Logger LOG = LoggerFactory.getLogger(OpenWireInactivityMonitor.class);

    private final AtomicBoolean commandSent = new AtomicBoolean(false);
    private final AtomicBoolean inSend = new AtomicBoolean(false);
    protected WireFormat wireFormat;
    private long writeCheckTime = DEFAULT_CHECK_TIME_MILLS;
    private ScheduledFuture writeFuture;

    public OpenWireInactivityMonitor(AsyncExecutors asyncExecutors, ProtocolTransport transport) {
        super(asyncExecutors, transport);
    }

    protected void doStart() {
        writeCheckTime = readCheckTime > 3 ? readCheckTime / 3 : readCheckTime;
        startReadCheck();
        Runnable writer = new Runnable() {
            @Override
            public void run() {
                writeCheck();
            }
        };
        writeFuture = asyncExecutors.scheduleAtFixedRate(writer, writeCheckTime, DEFAULT_MAX_COMPLETION);
    }

    protected void doStop(ServiceStopper stopper) {
        stopWriteCheck();
        super.doStop(stopper);
    }

    public void startWrite() {
        inSend.set(true);
    }

    public void finishedWrite() {
        inSend.set(false);
        commandSent.set(true);
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
}
