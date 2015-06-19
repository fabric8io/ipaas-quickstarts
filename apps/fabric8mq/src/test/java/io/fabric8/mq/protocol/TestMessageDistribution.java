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

import io.fabric8.mq.MessageDistribution;
import io.fabric8.mq.util.TransportConnectionState;
import io.fabric8.mq.util.TransportConnectionStateRegister;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.Command;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.command.ProducerInfo;
import org.apache.activemq.command.SessionInfo;
import org.apache.activemq.state.ConsumerState;
import org.apache.activemq.state.ProducerState;
import org.apache.activemq.state.SessionState;
import org.apache.activemq.transport.ResponseCallback;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.transport.TransportFactory;
import org.apache.activemq.transport.TransportListener;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

public class TestMessageDistribution extends ServiceSupport implements MessageDistribution {
    private static Logger LOG = LoggerFactory.getLogger(TestMessageDistribution.class);
    private final TransportConnectionStateRegister transportConnectionStateRegister = new TransportConnectionStateRegister();
    private final InternalTransportListener internalTransportListener = new InternalTransportListener();
    private final BrokerService brokerService = new BrokerService();
    private Transport transport;

    public TestMessageDistribution() {
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
        transport.oneway(command);
    }

    @Override
    public void send(ActiveMQDestination destination, Command command) throws IOException {
        transport.oneway(command);
    }

    @Override
    public void asyncSendAll(final Command command, final ResponseCallback callback) throws IOException {
        transport.asyncRequest(command, callback);
    }

    @Override
    public void asyncSend(ActiveMQDestination destination, Command command, ResponseCallback callback) throws IOException {
        transport.asyncRequest(command, callback);
    }

    @Override
    public TransportListener getTransportListener() {
        return internalTransportListener;
    }

    @Override
    public void setTransportListener(TransportListener transportListener) {
        internalTransportListener.setTransportListener(transportListener);
    }

    @Override
    public void transportCreated(String brokerId, Transport transport) {
    }

    @Override
    public void transportDestroyed(String brokerIdl) {

    }

    @Override
    public int getCurrentConnectedBrokerCount() {
        return 1;
    }

    @Override
    protected void doStart() throws Exception {
        brokerService.setBrokerId("test");
        brokerService.setBrokerId("test");
        brokerService.setPersistent(false);
        brokerService.addConnector("tcp://localhost:0");
        brokerService.start();
        brokerService.waitUntilStarted();
        String uriString = brokerService.getDefaultSocketURIString();
        transport = createTransport(uriString);

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
    }

    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        if (transport != null) {
            transport.stop();
        }
        if (brokerService != null) {
            brokerService.stop();
        }
    }

    public String getBrokerURI() {
        return brokerService.getDefaultSocketURIString();
    }

    public Transport createTransport(String uri) throws Exception {
        URI location = new URI("failover:(" + uri + "?wireFormat.cacheEnabled=false)?maxReconnectAttempts=0");
        TransportFactory factory = TransportFactory.findTransportFactory(location);
        final Transport transport = factory.doConnect(location);
        transport.setTransportListener(new TransportListener() {
            private final TransportListener transportListener = getTransportListener();

            public void onCommand(Object o) {
                transportListener.onCommand(o);
            }

            public void onException(IOException e) {
                transportListener.onException(e);
            }

            public void transportInterupted() {
                transportListener.transportInterupted();
            }

            public void transportResumed() {
                transportListener.transportResumed();
            }
        });
        transport.start();
        return transport;
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


