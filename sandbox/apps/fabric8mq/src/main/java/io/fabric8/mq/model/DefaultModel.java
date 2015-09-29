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

package io.fabric8.mq.model;

import com.codahale.metrics.JmxReporter;
import io.fabric8.mq.AsyncExecutors;
import io.fabric8.mq.MessageDistribution;
import io.fabric8.mq.coordination.brokers.BrokerDestinationOverviewMBean;
import io.fabric8.mq.coordination.brokers.BrokerModel;
import io.fabric8.mq.coordination.brokers.BrokerOverview;
import io.fabric8.mq.multiplexer.Multiplexer;
import io.fabric8.mq.multiplexer.MultiplexerInput;
import io.fabric8.mq.util.MoveDestinationWorker;
import io.fabric8.utils.JMXUtils;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.filter.DestinationMap;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
@Default
public class DefaultModel extends ServiceSupport implements Model {
    private static Logger LOG = LoggerFactory.getLogger(DefaultModel.class);
    private final ConcurrentMap<Object, ObjectName> objectNameMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, BrokerModel> brokerModelMap = new ConcurrentHashMap<>();
    private final DestinationMap destinationMap = new DestinationMap();
    private final DestinationController destinationController = new DestinationController();

    @Inject
    BrokerLimitsConfig brokerLimitsConfig;
    @Inject
    AsyncExecutors asyncExecutors;
    private JmxReporter jmxReporter = JmxReporter.forRegistry(METRIC_REGISTRY).inDomain(DEFAULT_JMX_DOMAIN).build();

    public BrokerLimitsConfig getBrokerLimitsConfig() {
        return brokerLimitsConfig;
    }

    @Override
    public void add(Multiplexer multiplexer) {
        try {
            ObjectName objectName = new ObjectName(DEFAULT_JMX_DOMAIN, "name", multiplexer.getName());
            registerInJmx(objectName, multiplexer);
        } catch (Throwable e) {
            LOG.error("Failed to register " + multiplexer, e);
        }
    }

    @Override
    public void remove(Multiplexer multiplexer) {
        unregisterInJmx(multiplexer);
    }

    @Override
    public void add(MultiplexerInput multiplexerInput) {
        try {
            ObjectName objectName = new ObjectName(DEFAULT_JMX_DOMAIN, "name", multiplexerInput.getName());
            registerInJmx(objectName, multiplexerInput);
        } catch (Throwable e) {
            LOG.error("Failed to register " + multiplexerInput, e);
        }
    }

    @Override
    public void remove(MultiplexerInput multiplexerInput) {
        unregisterInJmx(multiplexerInput);
        destinationController.unregister(multiplexerInput);
    }

    @Override
    public void add(BrokerModel brokerModel) {
        if (brokerModelMap.putIfAbsent(brokerModel.getBrokerId(), brokerModel) == null) {
            try {
                String name = getClass().getPackage().getName() + ".broker." + brokerModel.getBrokerId();
                name = ObjectName.quote(name);
                ObjectName objectName = new ObjectName(DEFAULT_JMX_DOMAIN, "name", name);
                registerInJmx(objectName, brokerModel);
            } catch (Throwable e) {
                LOG.error("Failed to register " + brokerModel, e);
            }
        }
    }

    @Override
    public void remove(BrokerModel brokerModel) {
        if (brokerModel != null) {
            if (brokerModelMap.remove(brokerModel.getBrokerId()) != null) {
                brokerModel.unlockWriteLock();
                brokerModel.unlockReadLock();
                unregisterInJmx(brokerModel);
            }
        }
    }

    @Override
    public void register(MultiplexerInput multiplexerInput, DestinationStatisticsMBean destinationStatistics) {
        try {
            ObjectName objectName = new ObjectName(DEFAULT_JMX_DOMAIN, "name", destinationStatistics.getName());
            registerInJmx(objectName, destinationStatistics);
            destinationStatistics.start();
        } catch (Throwable e) {
            LOG.error("Failed to register " + destinationStatistics, e);
        }
    }

    @Override
    public void unregister(MultiplexerInput multiplexer, DestinationStatisticsMBean destinationStatistics) {
        try {
            destinationStatistics.stop();
            unregisterInJmx(destinationStatistics);
        } catch (Throwable e) {
            LOG.error("Failed to unregister " + destinationStatistics, e);
        }
    }

    @Override
    public BrokerModel getMostLoadedBroker() {
        BrokerModel result = null;
        if (!brokerModelMap.isEmpty()) {
            List<BrokerModel> list = new ArrayList<>(brokerModelMap.values());
            if (!list.isEmpty()) {
                if (list.size() > 1) {
                    Collections.sort(list, new BrokerComparable());
                    result = list.get(list.size() - 1);
                } else {
                    result = list.get(0);
                }
            }
        }
        return result;
    }

    @Override
    public BrokerModel getLeastLoadedBroker() {
        BrokerModel result = null;
        if (!brokerModelMap.isEmpty()) {
            List<BrokerModel> list = new ArrayList<>(brokerModelMap.values());
            if (!list.isEmpty()) {
                if (list.size() > 1) {
                    Collections.sort(list, new BrokerComparable());
                }
                result = list.get(0);
            }
        }
        return result;
    }

    @Override
    public BrokerModel getNextLeastLoadedBroker(BrokerModel brokerModel) {
        BrokerModel result = null;
        if (!brokerModelMap.isEmpty()) {
            List<BrokerModel> list = new ArrayList<>(brokerModelMap.values());
            if (!list.isEmpty()) {
                if (list.size() > 1) {
                    Collections.sort(list, new BrokerComparable());
                }

                for (int i = 0; i < list.size(); i++) {
                    BrokerModel bm = list.get(i);
                    if (bm.equals(brokerModel)) {
                        int offset = i + 1;
                        if (list.size() > offset) {
                            result = list.get(offset);
                        }
                        break;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Collection<BrokerModel> getBrokers() {
        return brokerModelMap.values();
    }

    @Override
    public int getBrokerCount() {
        return brokerModelMap.size();
    }

    @Override
    public BrokerModel getBrokerById(String id) {
        return brokerModelMap.get(id);
    }

    @Override
    public List<ActiveMQDestination> getSortedDestinations(BrokerModel brokerModel, int maxNumber) {
        DestinationComparable destinationComparable = new DestinationComparable(brokerModel);
        return destinationComparable.getSortedDestinations(maxNumber);
    }

    @Override
    public List<ActiveMQDestination> getSortedDestinations(BrokerModel brokerModel) {
        DestinationComparable destinationComparable = new DestinationComparable(brokerModel);
        return destinationComparable.getSortedDestinations();
    }

    @Override
    public Set<ActiveMQDestination> getActiveDestinations(BrokerModel brokerModel) {
        return brokerModel.getActiveDestinations();
    }

    @Override
    public boolean areBrokerLimitsExceeded(BrokerModel brokerModel) {
        if (brokerModel != null) {
            BrokerOverview brokerOverview = brokerModel.getBrokerOverview();
            if (brokerOverview != null) {
                String brokerName = brokerModel.getBrokerId();
                int totalConnections = brokerOverview.getTotalConnections();
                int maxConnectionsPerBroker = brokerLimitsConfig.getMaxConnectionsPerBroker();
                boolean connectionsExceeded = totalConnections > maxConnectionsPerBroker;
                if (connectionsExceeded) {
                    LOG.info("Broker " + brokerName + " EXCEEDED connection limits(" + maxConnectionsPerBroker + ") with " + totalConnections + " connections");
                }

                int totalDestinations = brokerOverview.getTotalActiveDestinations();
                int maxDestinationsPerBroker = brokerLimitsConfig.getMaxDestinationsPerBroker();
                boolean destinationsExceeded = totalDestinations > maxDestinationsPerBroker;

                if (destinationsExceeded) {
                    LOG.info("Broker " + brokerName + " EXCEEDED destination limits(" + maxDestinationsPerBroker + ") with " + totalDestinations + " active destinations");
                }
                return connectionsExceeded || destinationsExceeded;
            }
        }
        return false;
    }

    @Override
    public boolean areBrokerConnectionLimitsExceeded(BrokerModel brokerModel) {
        boolean result = false;
        if (brokerModel != null) {
            BrokerOverview brokerOverview = brokerModel.getBrokerOverview();
            if (brokerOverview != null) {
                String brokerName = brokerModel.getBrokerId();
                int totalConnections = brokerOverview.getTotalConnections();
                int maxConnectionsPerBroker = brokerLimitsConfig.getMaxConnectionsPerBroker();
                boolean connectionsExceeded = totalConnections > maxConnectionsPerBroker;
                if (connectionsExceeded) {
                    LOG.info("Broker " + brokerName + " EXCEEDED connection limits(" + maxConnectionsPerBroker + ") with " + totalConnections + " connections");
                    result = true;
                }
            }
        }
        return result;
    }

    @Override
    public int spareConnections(BrokerModel brokerModel) {
        int result = 0;
        BrokerOverview brokerOverview = brokerModel.getBrokerOverview();
        if (brokerOverview != null) {
            result = brokerLimitsConfig.getMaxConnectionsPerBroker() - brokerOverview.getTotalConnections();
        }
        return result;
    }

    @Override
    public boolean areDestinationLimitsExceeded(BrokerModel brokerModel) {
        if (brokerModel != null) {
            BrokerOverview brokerOverview = brokerModel.getBrokerOverview();
            if (brokerOverview != null) {
                int totalDestinations = brokerOverview.getTotalActiveDestinations();
                int maxDestinationsPerBroker = brokerLimitsConfig.getMaxDestinationsPerBroker();
                boolean destinationsExceeded = totalDestinations > maxDestinationsPerBroker;
                String brokerName = brokerModel.getBrokerId();
                if (destinationsExceeded) {
                    LOG.info("Broker " + brokerName + " EXCEEDED destination limits(" + maxDestinationsPerBroker + ") with " + totalDestinations + " active destinations");
                    return true;
                }
                for (BrokerDestinationOverviewMBean brokerDestinationOverview : brokerOverview.getQueueOverviews().values()) {
                    if (brokerDestinationOverview.getQueueDepth() > brokerLimitsConfig.getMaxDestinationDepth()) {
                        return true;
                    }
                    if (brokerDestinationOverview.getNumberOfProducers() > brokerLimitsConfig.getMaxProducersPerDestination()) {
                        return true;
                    }
                    if (brokerDestinationOverview.getNumberOfConsumers() > brokerLimitsConfig.getMaxConsumersPerDestination()) {
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    @Override
    public int spareDestinations(BrokerModel brokerModel) {
        int result = 0;
        BrokerOverview brokerOverview = brokerModel.getBrokerOverview();
        if (brokerOverview != null) {
            result = brokerLimitsConfig.getMaxDestinationsPerBroker() - brokerOverview.getTotalActiveDestinations();
        }
        return result;
    }

    @Override
    public boolean isMaximumNumberOfBrokersReached() {
        return getBrokerCount() >= brokerLimitsConfig.getMaxNumberOfBrokers();
    }

    @Override
    public int getLoad(BrokerModel brokerModel) {
        int load = 0;
        if (brokerModel != null) {
            BrokerOverview brokerOverview = brokerModel.getBrokerOverview();
            if (brokerOverview != null) {
                int numConnections = brokerOverview.getTotalConnections();
                int connectionLoad = numConnections > 0 ? (numConnections * 30) / brokerLimitsConfig.getMaxConnectionsPerBroker() : 0;
                int numDestinations = brokerOverview.getTotalActiveDestinations();
                int destinationLoad = numDestinations > 0 ? (numDestinations * 35) / brokerLimitsConfig.getMaxDestinationsPerBroker() : 0;
                int queueDepth = brokerOverview.getTotalQueueDepth();
                int queueDepthLoad = queueDepth > 0 ? (queueDepth * 35) / brokerLimitsConfig.getMaxDestinationDepth() : 0;
                return destinationLoad + queueDepthLoad;
            }
        }
        return load;
    }

    @Override
    public Set<BrokerModel> getBrokersForDestination(ActiveMQDestination destination) {
        return (Set<BrokerModel>) destinationMap.get(destination);
    }

    @Override
    public void addBrokerForDestination(ActiveMQDestination destination, BrokerModel brokerModel) {
        destinationMap.put(destination, brokerModel);
    }

    @Override
    public BrokerModel addBrokerForDestination(ActiveMQDestination destination) {
        BrokerModel brokerModel = getLeastLoadedBroker();
        if (brokerModel != null) {
            destinationMap.put(destination, brokerModel);
        }
        return brokerModel;
    }

    @Override
    public void removeBrokerFromDestination(ActiveMQDestination destination, BrokerModel brokerModel) {
        destinationMap.remove(destination, brokerModel);
    }

    @Override
    public boolean copyDestinations(BrokerModel from, BrokerModel to) {
        List<ActiveMQDestination> list = new ArrayList<>(from.getActiveDestinations());
        return copyDestinations(from, to, list);
    }

    @Override
    public boolean copyDestinations(BrokerModel from, BrokerModel to, Collection<ActiveMQDestination> destinations) {
        boolean result = false;
        if (!destinations.isEmpty()) {
            Collection<MessageDistribution> messageDistributions = null;
            try {
                for (ActiveMQDestination destination : destinations) {
                    stopDispatching(destination);
                    LOG.info("Moving  " + from.getBrokerName() + " TO " + to.getBrokerName() + " : " + destination);
                }
                List<ActiveMQDestination> copy = new CopyOnWriteArrayList<>(destinations);
                do {
                    for (ActiveMQDestination destination : copy) {
                        if (isStoppedDispatching(destination)) {
                            copy.remove(destination);
                        }
                    }
                    if (!copy.isEmpty()) {
                        Thread.sleep(1000);
                    }
                } while (!copy.isEmpty());

                from.getWriteLock();
                to.getWriteLock();

                messageDistributions = from.detachTransport();
                //move the queues
                MoveDestinationWorker moveDestinationWorker = new MoveDestinationWorker(asyncExecutors, from, to);
                for (ActiveMQDestination destination : destinations) {
                    moveDestinationWorker.addDestinationToCopy(destination);
                    removeBrokerFromDestination(destination, from);
                }
                moveDestinationWorker.start();
                if (moveDestinationWorker.aWait(10, TimeUnit.MINUTES)) {
                    //update the sharding map
                    for (ActiveMQDestination destination : destinations) {
                        addBrokerForDestination(destination, to);
                    }
                    result = true;
                }

            } catch (Throwable e) {
                LOG.error("Failed in copy from " + from + " to " + to, e);
            } finally {
                if (messageDistributions != null) {
                    from.attachTransport(messageDistributions);
                }
                from.unlockWriteLock();
                to.unlockWriteLock();
                for (ActiveMQDestination destination : destinations) {
                    startDispatching(destination);
                }
            }
        } else {
            result = true;
        }
        return result;
    }

    @Override
    protected void doStart() throws Exception {
        jmxReporter.start();
        asyncExecutors.start();
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        jmxReporter.stop();
    }

    public void registerInJmx(ObjectName objectName, Object object) throws Exception {
        JMXUtils.registerMBean(object, objectName);
        objectNameMap.put(object, objectName);
    }

    public void unregisterInJmx(Object object) {
        ObjectName objectName = objectNameMap.remove(object);
        if (objectName != null) {
            JMXUtils.unregisterMBean(objectName);
        }
    }

    @Override
    public void dispatched(MultiplexerInput MultiplexerInput, ActiveMQDestination destination) {
        destinationController.dispatched(MultiplexerInput, destination);
    }

    @Override
    public void acked(MultiplexerInput MultiplexerInput, ActiveMQDestination destination) {
        destinationController.acked(MultiplexerInput, destination);
    }

    @Override
    public void unregister(MultiplexerInput MultiplexerInput) {
        destinationController.unregister(MultiplexerInput);
    }

    @Override
    public void stopDispatching(ActiveMQDestination destination) {
        destinationController.stopDispatching(destination);
    }

    @Override
    public void startDispatching(ActiveMQDestination destination) {
        destinationController.startDispatching(destination);
    }

    @Override
    public boolean canDispatch(ActiveMQDestination destination) {
        return destinationController.canDispatch(destination);
    }

    @Override
    public boolean isStoppedDispatching(ActiveMQDestination destination) {
        return destinationController.isStoppedDispatching(destination);
    }

    private class BrokerComparable implements Comparator<BrokerModel> {

        @Override
        public int compare(BrokerModel broker1, BrokerModel broker2) {
            int result = 0;
            if (broker1 != broker2) {
                result = getLoad(broker1) - getLoad(broker2);
            }
            return result;
        }
    }

    private class DestinationComparable implements Comparator<ActiveMQDestination> {

        private final BrokerModel brokerModel;

        DestinationComparable(BrokerModel brokerModel) {
            this.brokerModel = brokerModel;
        }

        List<ActiveMQDestination> getSortedDestinations() {
            List<ActiveMQDestination> list = new ArrayList<>(brokerModel.getActiveDestinations());
            Collections.sort(list, this);
            return list;
        }

        List<ActiveMQDestination> getSortedDestinations(int max) {
            List<ActiveMQDestination> list = new ArrayList<>(brokerModel.getActiveDestinations());
            Collections.sort(list, this);
            if (max > 0 && list.size() > max) {
                while (list.size() > max) {
                    list.remove(list.size() - 1);
                }
            }
            return list;
        }

        @Override
        public int compare(ActiveMQDestination dest1, ActiveMQDestination dest2) {
            int depth1 = brokerModel.getDepth(dest1);
            int depth2 = brokerModel.getDepth(dest2);
            int result = depth1 - depth2;
            if (result == 0) {
                result = brokerModel.getNumberOfProducers(dest1) - brokerModel.getNumberOfProducers(dest2);
                if (result == 0) {
                    result = brokerModel.getNumberOfConsumers(dest1) - brokerModel.getNumberOfConsumers(dest2);
                }
            }
            return result;
        }
    }

}
