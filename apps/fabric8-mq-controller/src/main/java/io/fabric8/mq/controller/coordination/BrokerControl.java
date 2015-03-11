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

package io.fabric8.mq.controller.coordination;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.mq.controller.BrokerStateInfo;
import io.fabric8.mq.controller.sharding.MessageDistribution;
import io.fabric8.mq.controller.util.TransportConnectionState;
import io.fabric8.utils.Systems;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.activemq.command.SessionInfo;
import org.apache.activemq.filter.DestinationMap;
import org.apache.activemq.state.ConsumerState;
import org.apache.activemq.state.ProducerState;
import org.apache.activemq.state.SessionState;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.transport.TransportFactory;
import org.apache.activemq.transport.TransportListener;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BrokerControl extends ServiceSupport implements BrokerChangeListener {
    private static Logger LOG = LoggerFactory.getLogger(BrokerControl.class);
    static final long GC_TIME = 60 * 1000;
    private final BrokerStateInfo brokerStateInfo;
    private DestinationMap destinationMap = new DestinationMap();
    private Map<String, BrokerView> brokerViewMap = new ConcurrentHashMap<>();
    private final List<MessageDistribution> messageDistributionList = new CopyOnWriteArrayList();
    private final Lock lock = new ReentrantLock(true);
    private String brokerCoordinatorType = "singleton";
    private ScheduledFuture gcFuture;
    private BrokerCoordinator brokerCoordinator;
    private int brokerId;

    public BrokerControl(BrokerStateInfo brokerStateInfo) {
        this.brokerStateInfo = brokerStateInfo;
    }

    public String getBrokerCoordinatorType() {
        return brokerCoordinatorType;
    }

    public void setBrokerCoordinatorType(String brokerCoordinatorType) {
        this.brokerCoordinatorType = brokerCoordinatorType;
    }

    @Override
    public void created(BrokerView brokerView) {
        localUpdate(brokerView);
    }

    @Override
    public void deleted(String brokerName) {
        localDelete(brokerName);
    }

    @Override
    public void updated(BrokerView brokerView) {
        localUpdate(brokerView);
    }

    public BrokerView getBrokerView(ActiveMQDestination destination) {
        BrokerView result = null;
        try {
            if (getLock()) {
                Set set = destinationMap.get(destination);
                if (set != null && !set.isEmpty()) {
                    result = (BrokerView) set.iterator().next();
                } else {
                    //create a broker
                }
            }
        } finally {
            releaseLock();
        }
        return result;
    }

    public void addMessageDistribution(MessageDistribution messageDistribution) {
        messageDistributionList.add(messageDistribution);
        if (isStarted()) {
            for (BrokerView brokerView : brokerViewMap.values()) {
                try {
                    createTransport(messageDistribution, brokerView, brokerView.getUri());
                } catch (Exception e) {
                    LOG.warn("Failed to create transport to " + brokerView + " for " + messageDistribution);
                }
            }
        }

    }

    public void removeMessageDistribution(MessageDistribution messageDistribution) {
        messageDistributionList.remove(messageDistribution);
        for (BrokerView brokerView : brokerViewMap.values()) {
            brokerView.removeTransport(messageDistribution);
        }

    }

    public Collection<BrokerView> getBrokerViews() {
        return brokerViewMap.values();
    }

    public synchronized BrokerView createBroker() {
        BrokerView result = null;
        String brokerName = "broker-" + this.brokerStateInfo.getController().getName() + "-" + (brokerId++);
        //get the interProcessMutex
        try {

            //ToDo use Kube to do the create broker
            try {
                BrokerService brokerService = new BrokerService();
                brokerService.setBrokerName(brokerName);
                brokerService.setPersistent(false);
                brokerService.setUseJmx(false);
                brokerService.addConnector("tcp://localhost:0");
                brokerService.start();
                String uri = brokerService.getDefaultSocketURIString();
                result = new BrokerView();
                result.setBrokerId(brokerService.getBroker().getBrokerId().toString());
                result.setBrokerName(brokerService.getBrokerName());
                result.setUri(uri);

                brokerCoordinator.createBroker(result);

            } catch (Throwable e) {
                result = null;
                LOG.error("Failed to create broker", e);
            }

        } catch (Throwable e) {
            LOG.error("Failed to get interProcessMutex");
        }

        return result;
    }

    public void localDelete(String brokerName) {
        BrokerView brokerView = brokerViewMap.remove(brokerName);
        if (brokerView != null) {
            try {
                if (getLock()) {
                    ActiveMQDestination dest = ActiveMQDestination.createDestination(">", ActiveMQDestination.QUEUE_TYPE);
                    destinationMap.remove(dest, brokerView);
                    dest = ActiveMQDestination.createDestination(">", ActiveMQDestination.TOPIC_TYPE);
                    destinationMap.remove(dest, brokerView);
                }
            } finally {
                releaseLock();
            }
        }
    }

    @Override
    protected void doStart() throws Exception {

        String brokerCoordinatorType = Systems.getEnvVarOrSystemProperty("BROKER_COORDINATOR_TYPE", getBrokerCoordinatorType());
        brokerCoordinator = BrokerCoordinatorFactory.getCoordinator(brokerCoordinatorType);
        brokerCoordinator.addBrokerChangeListener(this);
        brokerCoordinator.start();

        if (brokerStateInfo != null && brokerStateInfo.getController() != null) {
            Runnable runnable = new Runnable() {

                @Override
                public void run() {
                    for (BrokerView brokerView : brokerViewMap.values()) {
                        brokerView.markSweep();
                        if (brokerView.isGC() && brokerViewMap.size() > 1) {
                            brokerCoordinator.deleteBroker(brokerView);
                        }
                    }
                }
            };
            gcFuture = brokerStateInfo.getController().scheduleAtFixedRate(runnable, GC_TIME, GC_TIME);
        }

        if (brokerViewMap.isEmpty()) {
            BrokerView topicBroker = createBroker();
            BrokerView queueBroker = createBroker();
            try {
                if (getLock()) {

                    ActiveMQDestination dest = ActiveMQDestination.createDestination(">", ActiveMQDestination.QUEUE_TYPE);
                    destinationMap.put(dest, queueBroker);
                    dest = ActiveMQDestination.createDestination(">", ActiveMQDestination.TOPIC_TYPE);
                    destinationMap.put(dest, topicBroker);
                }
            } finally {
                releaseLock();
            }
        }
    }

    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        BrokerCoordinator bc = brokerCoordinator;
        if (bc != null) {
            bc.stop();
        }
        if (gcFuture != null) {
            gcFuture.cancel(true);
        }
    }

    private void localUpdate(byte[] data) {
        try {
            if (data != null) {
                ObjectMapper objectMapper = new ObjectMapper();
                BrokerView brokerView = objectMapper.readValue(data, BrokerView.class);
                if (brokerView != null) {
                    localUpdate(brokerView);
                }
            }
        } catch (Throwable e) {
            LOG.error("Failed to update BrokerInfo ", e);
        }
    }

    private void localUpdate(BrokerView updatedBrokerView) {
        try {
            if (updatedBrokerView != null && updatedBrokerView.getBrokerName() != null) {
                BrokerView brokerView = brokerViewMap.get(updatedBrokerView.getBrokerName());
                if (brokerView == null) {
                    brokerViewMap.put(updatedBrokerView.getBrokerName(), updatedBrokerView);
                    brokerView = updatedBrokerView;
                    String address = brokerView.getUri();
                    createTransports(brokerView, address);

                }
                brokerView.updateDestinations(updatedBrokerView);

                List<String> destinations = brokerView.getDestinations();
                if (destinations != null) {
                    for (String str : destinations) {
                        ActiveMQDestination destination = ActiveMQDestination.createDestination(str, ActiveMQDestination.QUEUE_TYPE);

                        try {
                            if (getLock()) {
                                destinationMap.put(destination, brokerView);
                            }
                        } finally {
                            releaseLock();
                        }

                    }
                }
            }
        } catch (Throwable e) {
            LOG.error("Failed to update BrokerView", e);
        }
    }

    private void localDelete(byte[] data) {
        try {
            if (data != null) {
                ObjectMapper objectMapper = new ObjectMapper();
                BrokerView brokerView = objectMapper.readValue(data, BrokerView.class);
                if (brokerView != null) {
                    localDelete(brokerView.getBrokerName());
                }
            }
        } catch (Throwable e) {
            LOG.error("Failed to delete BrokerInfo ", e);
        }
    }

    private void createTransports(final BrokerView view, String address) throws Exception {
        for (final MessageDistribution messageDistribution : messageDistributionList) {
            Transport transport = createTransport(messageDistribution, view, address);

            List<TransportConnectionState> list = brokerStateInfo.getTransportConnectionStateRegister().listConnectionStates();
            for (TransportConnectionState transportConnectionState : list) {
                ConnectionInfo connectionInfo = transportConnectionState.getInfo();
                transport.oneway(connectionInfo);
                Collection<SessionState> collection = transportConnectionState.getSessionStates();
                for (SessionState sessionState : collection) {
                    SessionInfo sessionInfo = sessionState.getInfo();
                    transport.oneway(sessionInfo);
                    for (ProducerState producerState : sessionState.getProducerStates()) {
                        transport.oneway(producerState.getInfo());
                    }
                    for (ConsumerState consumerState : sessionState.getConsumerStates()) {
                        transport.oneway(consumerState.getInfo());
                    }
                }
            }
        }
        LOG.info("Created transport(" + messageDistributionList.size() + ")s to " + address);
    }

    private boolean getLock() {
        try {
            while (!isStopping()) {

                if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
                    return true;
                }

            }
        } catch (Throwable e) {
        }
        return false;
    }

    private void releaseLock() {
        try {
            lock.unlock();
        } catch (Throwable e) {
        }
    }

    private Transport createTransport(final MessageDistribution messageDistribution, final BrokerView view, String address) throws Exception {

        URI location = new URI(address + "?wireFormat.cacheEnabled=false");
        TransportFactory factory = TransportFactory.findTransportFactory(location);
        final Transport transport = factory.doConnect(location);

        transport.setTransportListener(new TransportListener() {
            private final TransportListener transportListener = messageDistribution.getTransportListener();
            private final String name = view.getBrokerName();

            public void onCommand(Object o) {
                transportListener.onCommand(o);
            }

            public void onException(IOException e) {
                localDelete(name);
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
        view.addTransport(messageDistribution, transport);
        return transport;
    }
}


