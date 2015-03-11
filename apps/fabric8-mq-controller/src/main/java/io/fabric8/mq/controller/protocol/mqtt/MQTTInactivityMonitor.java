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

package io.fabric8.mq.controller.protocol.mqtt;

import io.fabric8.mq.controller.MQController;
import io.fabric8.mq.controller.protocol.InactivityMonitor;
import io.fabric8.mq.controller.protocol.ProtocolTransport;
import org.apache.activemq.transport.InactivityIOException;
import org.apache.activemq.util.ServiceStopper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;

public class MQTTInactivityMonitor extends InactivityMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(MQTTInactivityMonitor.class);

    private long connectionTimeout = DEFAULT_CHECK_TIME_MILLS;
    private long readGraceTime = DEFAULT_CHECK_TIME_MILLS;
    private long readKeepAliveTime = DEFAULT_CHECK_TIME_MILLS;
    private ScheduledFuture connectionFuture;
    private ScheduledFuture readFuture;

    public MQTTInactivityMonitor(MQController gateway, ProtocolTransport transport) {
        super(gateway, transport);
    }

    @Override
    public void doStart() {

    }

    @Override
    public void doStop(ServiceStopper stopper) {
        stopConnectionChecker();
        super.doStop(stopper);
    }

    synchronized void startConnectChecker(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        if (connectionTimeout > 0 && connectionFuture == null) {

            long connectionCheckInterval = Math.min(connectionTimeout, 1000);

            Runnable connection = new Runnable() {
                long now = System.currentTimeMillis();

                public void run() {
                    connectionCheck(now);
                }
            };
            connectionFuture = gateway.scheduleAtFixedRate(connection, connectionCheckInterval, DEFAULT_MAX_COMPLETION);
        }
    }

    synchronized void stopConnectionChecker() {
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
                LOG.debug("No CONNECT frame received in time for " + MQTTInactivityMonitor.this.toString() + "! Throwing InactivityIOException.");

                stopConnectionChecker();
                onException(new InactivityIOException("Channel was inactive for too (>" + (readKeepAliveTime + readGraceTime) + ") long: "
                                                          + transport.getRemoteAddress()));
            }
        }
    }
}
