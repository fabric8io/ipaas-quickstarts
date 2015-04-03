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

package io.fabric8.mq.controller.sharding;

import io.fabric8.mq.controller.BrokerStateInfo;
import io.fabric8.mq.controller.coordination.brokermodel.BrokerView;
import io.fabric8.mq.controller.util.LRUCache;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.Command;
import org.apache.activemq.command.ExceptionResponse;
import org.apache.activemq.transport.FutureResponse;
import org.apache.activemq.transport.ResponseCallback;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.transport.TransportListener;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ShardedMessageDistribution extends ServiceSupport implements MessageDistribution {
    private static Logger LOG = LoggerFactory.getLogger(ShardedMessageDistribution.class);
    private final BrokerStateInfo brokerStateInfo;
    private final Map<MultiCallback, MultiCallback> requestMap = new LRUCache<>(50000);
    private TransportListener transportListener;

    public ShardedMessageDistribution(BrokerStateInfo brokerStateInfo) {
        this.brokerStateInfo = brokerStateInfo;
    }

    @Override
    public void sendAll(Command command) throws IOException {
        if (isStarted()) {
            for (Transport transport : brokerStateInfo.getBrokerControl().getTransports(this)) {
                if (transport != null) {
                    transport.oneway(command);
                }
            }
        }
    }

    @Override
    public void send(ActiveMQDestination destination, Command command) throws IOException {
        if (isStarted()) {
            Transport transport = brokerStateInfo.getBrokerControl().getTransport(this, destination);
            if (transport != null) {
                transport.oneway(command);
            }
        }
    }

    @Override
    public void asyncSendAll(final Command command, final ResponseCallback callback) throws IOException {
        MultiCallback multiCallback = new MultiCallback(command, callback);
        synchronized (requestMap) {
            requestMap.put(multiCallback, multiCallback);
        }

        if (isStarted()) {
            for (Transport transport : brokerStateInfo.getBrokerControl().getTransports(this)) {
                if (transport != null) {
                    transport.asyncRequest(command, multiCallback);
                }
            }
        }
    }

    @Override
    public void asyncSend(ActiveMQDestination destination, Command command, ResponseCallback callback) throws IOException {
        if (isStarted()) {
            Transport transport = brokerStateInfo.getBrokerControl().getTransport(this, destination);
            if (transport != null) {
                transport.asyncRequest(command, callback);
            }
        }
    }

    @Override
    public TransportListener getTransportListener() {
        return transportListener;
    }

    @Override
    public void setTransportListener(TransportListener transportListener) {
        this.transportListener = transportListener;
    }

    @Override
    protected void doStart() throws Exception {
        brokerStateInfo.getBrokerControl().addMessageDistribution(this);
    }

    protected void doStop(ServiceStopper serviceStopper) throws IOException {
        brokerStateInfo.getBrokerControl().removeMessageDistribution(this);
        IOException stopped = new IOException("stopped");
        ArrayList<MultiCallback> requests = null;
        synchronized (requestMap) {
            requests = new ArrayList<>(requestMap.keySet());
            requestMap.clear();

        }
        if (requests != null) {
            for (MultiCallback multiCallback : requests) {
                FutureResponse futureResponse = new FutureResponse(multiCallback.getCallback());
                futureResponse.set(new ExceptionResponse(stopped));
                multiCallback.onCompletion(futureResponse);
            }
        }
    }

    private Transport getTransport(BrokerView brokerView) {
        Transport result = null;
        if (brokerView != null) {
            result = brokerView.getTransport(this);
        }
        return result;
    }

    private class MultiCallback implements ResponseCallback {
        private final AtomicBoolean called = new AtomicBoolean();
        private final Command command;
        private final ResponseCallback callback;

        MultiCallback(Command command, ResponseCallback callback) {
            this.command = command;
            this.callback = callback;
        }

        ResponseCallback getCallback() {
            return callback;
        }

        public void onCompletion(FutureResponse futureResponse) {
            if (called.compareAndSet(false, true)) {
                synchronized (requestMap) {
                    requestMap.remove(this);
                }
                callback.onCompletion(futureResponse);
            }
        }

    }
}


