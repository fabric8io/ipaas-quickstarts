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

import io.fabric8.mq.controller.MessageDistribution;
import io.fabric8.mq.controller.TransportChangedListener;
import io.fabric8.mq.controller.coordination.brokers.BrokerTransport;
import io.fabric8.mq.controller.model.BrokerControl;
import io.fabric8.mq.controller.model.BrokerModelChangedListener;
import io.fabric8.mq.controller.util.LRUCache;
import io.fabric8.mq.controller.util.MultiCallback;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ShardedMessageDistribution extends ServiceSupport implements MessageDistribution, BrokerModelChangedListener {
    private static Logger LOG = LoggerFactory.getLogger(ShardedMessageDistribution.class);
    private final BrokerControl brokerControl;
    private final List<TransportChangedListener> transportChangedListeners = new CopyOnWriteArrayList<>();
    private final Map<MultiCallback, MultiCallback> requestMap = new LRUCache<>(50000);
    private final InternalTransportListener listener = new InternalTransportListener();
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    public ShardedMessageDistribution(BrokerControl brokerControl) {
        this.brokerControl = brokerControl;
    }

    @Override
    public void sendAll(Command command) throws IOException {
        sendAll(command, false);
    }

    @Override
    public void sendAll(Command command, boolean force) throws IOException {
        if (!force) {
            waitForBroker();
        }
        if (isStarted()) {
            Collection<BrokerTransport> transports = brokerControl.getTransports(this);
            for (BrokerTransport brokerTransport : transports) {
                if (brokerTransport != null) {
                    brokerTransport.getTransport().oneway(command);
                    brokerTransport.release();
                }
            }
        } else {
            throw new IOException("ShardedMessageBroker not started");
        }
    }

    @Override
    public void send(ActiveMQDestination destination, Command command) throws IOException {
        waitForBroker();
        if (isStarted()) {
            BrokerTransport brokerTransport = brokerControl.getTransport(this, destination);
            if (brokerTransport != null) {
                brokerTransport.getTransport().oneway(command);
                brokerTransport.release();
            }
        } else {
            throw new IOException("ShardedMessageBroker not started");
        }
    }

    @Override
    public void asyncSendAll(final Command command, final ResponseCallback callback) throws IOException {
        waitForBroker();
        if (isStarted()) {
            MultiCallback multiCallback = new MultiCallback(requestMap, command, callback);
            synchronized (requestMap) {
                requestMap.put(multiCallback, multiCallback);
            }
            Collection<BrokerTransport> brokerTransports = brokerControl.getTransports(this);
            for (BrokerTransport brokerTransport : brokerTransports) {
                if (brokerTransport != null) {
                    brokerTransport.getTransport().asyncRequest(command, multiCallback);
                    brokerTransport.release();
                }
            }
        } else {
            throw new IOException("ShardedMessageBroker not started");
        }
    }

    @Override
    public void asyncSend(ActiveMQDestination destination, Command command, ResponseCallback callback) throws IOException {
        waitForBroker();
        if (isStarted()) {
            BrokerTransport brokerTransport = brokerControl.getTransport(this, destination);
            if (brokerTransport != null) {
                brokerTransport.getTransport().asyncRequest(command, callback);
                brokerTransport.release();
            }
        } else {
            throw new IOException("ShardedMessageBroker not started");
        }
    }

    @Override
    public TransportListener getTransportListener() {
        return listener;
    }

    @Override
    public void setTransportListener(TransportListener transportListener) {
        listener.setTransportListener(transportListener);
    }

    @Override
    public void addTransportCreatedListener(TransportChangedListener transportChangedListener) {
        transportChangedListeners.add(transportChangedListener);
    }

    @Override
    public void removeTransportCreatedListener(TransportChangedListener transportChangedListener) {
        transportChangedListeners.remove(transportChangedListener);
    }

    @Override
    public void transportCreated(String brokerId, Transport transport) {
        if (isStarted()) {
            for (TransportChangedListener transportChangedListener : transportChangedListeners) {
                transportChangedListener.transportCreated(brokerId, transport);
            }
        }

    }

    @Override
    public void transportDestroyed(String brokerIdl) {

    }

    @Override
    public int getCurrentConnectedBrokerCount() {
        return brokerControl.getBrokerModels().size();
    }

    @Override
    public void brokerNumberChanged(int numberOfBrokers) {
        if (numberOfBrokers > 0) {
            countDownLatch.countDown();
        }
    }

    @Override
    protected void doStart() throws Exception {
        brokerControl.addMessageDistribution(this);
        brokerControl.addBrokerModelChangedListener(this);
        if (!brokerControl.getBrokerModels().isEmpty()) {
            countDownLatch.countDown();
        }
    }

    protected void doStop(ServiceStopper serviceStopper) throws IOException {
        brokerControl.removeBrokerModelChangedListener(this);
        brokerControl.removeMessageDistribution(this);
        IOException stopped = new IOException("stopped");
        ArrayList<MultiCallback> requests;
        synchronized (requestMap) {
            requests = new ArrayList<>(requestMap.keySet());
            requestMap.clear();

        }
        for (MultiCallback multiCallback : requests) {
            FutureResponse futureResponse = new FutureResponse(multiCallback.getCallback());
            futureResponse.set(new ExceptionResponse(stopped));
            multiCallback.onCompletion(futureResponse);
        }

    }

    private void waitForBroker() {
        try {
            if (!countDownLatch.await(1, TimeUnit.MINUTES)) {
                throw new IllegalStateException("Broker didn't start");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private class InternalTransportListener implements TransportListener {
        private TransportListener listener;

        void setTransportListener(TransportListener l) {
            this.listener = l;
        }

        public void onCommand(Object command) {
            TransportListener l = this.listener;
            if (l != null) {
                l.onCommand(command);
            }
        }

        public void onException(IOException ex) {
            TransportListener l = this.listener;
            if (l != null) {
                l.onException(ex);
            }
        }

        public void transportInterupted() {
            TransportListener l = this.listener;
            if (l != null) {
                l.transportInterupted();
            }
        }

        public void transportResumed() {
            TransportListener l = this.listener;
            if (l != null) {
                l.transportResumed();
            }
        }
    }
}


