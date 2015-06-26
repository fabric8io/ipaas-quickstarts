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

package io.fabric8.mq.sharding;

import io.fabric8.mq.MessageDistribution;
import io.fabric8.mq.coordination.brokers.BrokerTransport;
import io.fabric8.mq.model.BrokerControl;
import io.fabric8.mq.model.BrokerModelChangedListener;
import io.fabric8.mq.util.LRUCache;
import io.fabric8.mq.util.MultiCallback;
import io.fabric8.mq.util.TransportConnectionState;
import io.fabric8.mq.util.TransportConnectionStateRegister;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.Command;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.command.ExceptionResponse;
import org.apache.activemq.command.ProducerInfo;
import org.apache.activemq.command.SessionInfo;
import org.apache.activemq.state.ConsumerState;
import org.apache.activemq.state.ProducerState;
import org.apache.activemq.state.SessionState;
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
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ShardedMessageDistribution extends ServiceSupport implements MessageDistribution, BrokerModelChangedListener {
    private static final Logger LOG = LoggerFactory.getLogger(ShardedMessageDistribution.class);
    private final BrokerControl brokerControl;
    private final TransportConnectionStateRegister transportConnectionStateRegister = new TransportConnectionStateRegister();
    private final Map<MultiCallback, MultiCallback> requestMap = new LRUCache<>(50000);
    private final InternalTransportListener listener = new InternalTransportListener();
    private final AtomicReference<CountDownLatch> attachedToBroker = new AtomicReference<>(new CountDownLatch(1));

    public ShardedMessageDistribution(BrokerControl brokerControl) {
        this.brokerControl = brokerControl;
    }

    @Override
    public TransportConnectionStateRegister getTransportConnectionStateRegister() {
        return transportConnectionStateRegister;
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
                try {
                    brokerTransport.lock();
                    brokerTransport.getTransport().oneway(command);
                } finally {
                    brokerTransport.unlock();
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
                try {
                    brokerTransport.lock();
                    brokerTransport.getTransport().oneway(command);
                } finally {
                    brokerTransport.unlock();
                }
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
                try {
                    brokerTransport.lock();
                    brokerTransport.getTransport().asyncRequest(command, multiCallback);
                } finally {
                    brokerTransport.unlock();
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
                try {
                    brokerTransport.lock();
                    brokerTransport.getTransport().asyncRequest(command, callback);
                } finally {
                    brokerTransport.unlock();
                }
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
    public void transportCreated(String brokerId, Transport transport) {
        if (isStarted()) {
            try {
                for (TransportConnectionState transportConnectionState : transportConnectionStateRegister.listConnectionStates()) {

                    ConnectionInfo connectionInfo = transportConnectionState.getInfo();
                    transport.oneway(connectionInfo);

                    int sessionCount = transportConnectionState.getSessionStates().size();
                    int consumerCount = 0;
                    int producerCount = 0;
                    for (SessionState sessionState : transportConnectionState.getSessionStates()) {
                        SessionInfo sessionInfo = sessionState.getInfo();
                        transport.oneway(sessionInfo);
                        consumerCount = sessionState.getConsumerStates().size();
                        for (ConsumerState consumerState : sessionState.getConsumerStates()) {
                            ConsumerInfo consumerInfo = consumerState.getInfo();
                            transport.oneway(consumerInfo);
                        }
                        producerCount = sessionState.getProducerStates().size();
                        for (ProducerState producerState : sessionState.getProducerStates()) {
                            ProducerInfo producerInfo = producerState.getInfo();
                            transport.oneway(producerInfo);
                        }
                    }
                    LOG.info("Sent to " + transport + " Connection Info " + connectionInfo.getClientId() + " [ sessions = " + sessionCount + ",consumers = " + consumerCount + ",producers=" + producerCount + "]");

                }
            } catch (Throwable e) {
                LOG.error("Failed to update connection state ", e);
            }
            CountDownLatch countDownLatch = attachedToBroker.get();
            countDownLatch.countDown();

        }

    }

    @Override
    public void transportDestroyed(String brokerIdl) {
        if (getCurrentConnectedBrokerCount() == 0) {
            attachedToBroker.set(new CountDownLatch(1));
        }
    }

    @Override
    public int getCurrentConnectedBrokerCount() {
        return brokerControl.getBrokerModels().size();
    }

    @Override
    public void brokerNumberChanged(int numberOfBrokers) {
    }

    @Override
    protected void doStart() throws Exception {
        brokerControl.addBrokerModelChangedListener(this);
        brokerControl.addMessageDistribution(this);
        if (!brokerControl.getBrokerModels().isEmpty()) {
            CountDownLatch countDownLatch = this.attachedToBroker.get();
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
            CountDownLatch countDownLatch = attachedToBroker.get();
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
            //we don't want to perculate this back
            //we've lost a Broker transport - it should
            //get attached again
        }

        public void transportInterupted() {
            //we don't want to perculate this back
            //we've lost a Broker transport - it should
            //get attached again
        }

        public void transportResumed() {
            //we don't want to perculate this back
            //we've lost a Broker transport - it should
            //get attached again
        }
    }
}


