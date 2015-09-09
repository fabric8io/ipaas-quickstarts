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

package io.fabric8.mq.coordination;

import io.fabric8.mq.AsyncExecutors;
import io.fabric8.mq.MessageDistribution;
import io.fabric8.mq.coordination.brokers.BrokerModel;
import io.fabric8.mq.coordination.brokers.BrokerTransport;
import io.fabric8.mq.coordination.brokers.DefaultBrokerTransport;
import io.fabric8.mq.coordination.scaling.ScalingEngine;
import io.fabric8.mq.coordination.scaling.ScalingEventListener;
import io.fabric8.mq.model.BrokerControl;
import io.fabric8.mq.model.BrokerModelChangedListener;
import io.fabric8.mq.model.Model;
import io.fabric8.mq.util.WorkInProgress;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

public abstract class BaseBrokerControl extends ServiceSupport implements BrokerControl, ScalingEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(BaseBrokerControl.class);
    @Inject
    protected Model model;
    @Inject
    protected AsyncExecutors asyncExecutors;
    @Inject
    protected ScalingEngine scalingEngine;
    protected List<MessageDistribution> messageDistributionList;
    protected WorkInProgress scalingInProgress;
    //protected Map<String, BrokerModel> brokerModelMap;
    protected BrokerCoordinator brokerCoordinator;
    protected List<BrokerModelChangedListener> brokerModelChangedListeners;
    private ScheduledFuture poller;
    @Inject
    @ConfigProperty(name = "BROKER_POLL_INTERVAL", defaultValue = "5000")
    private int pollTime;
    @Inject
    @ConfigProperty(name = "BROKER_NAME", defaultValue = "fabric8MQ-AMQ")
    private String brokerName;
    @Inject
    @ConfigProperty(name = "BROKER_GROUP", defaultValue = "mqGroup")
    private String groupName;
    @Inject
    @ConfigProperty(name = "BBROKER_TEMPLATE_LOCATION", defaultValue = "META-INF/replicator-template.json")
    private String brokerTemplateLocation;
    @Inject
    @ConfigProperty(name = "BROKER_SELECTOR", defaultValue = "component=amqbroker,provider=fabric8,group=amqbroker")
    private String brokerSelector;
    @Inject
    @ConfigProperty(name = "BBROKER_COORDINATOR", defaultValue = "singleton")
    private String brokerCoordinatorType;

    protected BaseBrokerControl() {
        messageDistributionList = new CopyOnWriteArrayList<>();
        scalingInProgress = new WorkInProgress();
        brokerModelChangedListeners = new CopyOnWriteArrayList<>();
    }

    protected abstract void pollBrokers();

    protected abstract void createBroker();

    protected abstract void destroyBroker(BrokerModel brokerModel);

    public String getBrokerSelector() {
        return brokerSelector;
    }

    public void setBrokerSelector(String brokerSelector) {
        this.brokerSelector = brokerSelector;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public int getPollTime() {
        return pollTime;
    }

    public void setPollTime(int pollTime) {
        this.pollTime = pollTime;
    }

    public String getBrokerTemplateLocation() {
        return brokerTemplateLocation;
    }

    public void setBrokerTemplateLocation(String brokerTemplateLocation) {
        this.brokerTemplateLocation = brokerTemplateLocation;
    }

    public String getBrokerCoordinatorType() {
        return brokerCoordinatorType;
    }

    public void setBrokerCoordinatorType(String brokerCoordinatorType) {
        this.brokerCoordinatorType = brokerCoordinatorType;
    }

    public Collection<BrokerModel> getBrokerModels() {
        return model.getBrokers();
    }

    @Override
    public Collection<BrokerTransport> getTransports(MessageDistribution messageDistribution) {
        List<BrokerTransport> list = new ArrayList<>();
        for (BrokerModel brokerModel : model.getBrokers()) {
            Transport transport = brokerModel.getTransport(messageDistribution);
            list.add(new DefaultBrokerTransport(brokerModel, transport));
        }
        return list;
    }

    @Override
    public BrokerTransport getTransport(MessageDistribution messageDistribution, ActiveMQDestination destination) {
        Set<BrokerModel> set = model.getBrokersForDestination(destination);
        if (set != null && !set.isEmpty()) {
            BrokerModel brokerModel = set.iterator().next();
            Transport transport = brokerModel.getTransport(messageDistribution);
            return new DefaultBrokerTransport(brokerModel, transport);
        } else {
            //allocate a broker for the destination
            BrokerModel brokerModel = model.addBrokerForDestination(destination);
            Transport transport = brokerModel.getTransport(messageDistribution);
            return new DefaultBrokerTransport(brokerModel, transport);
        }
    }

    @Override
    public void addMessageDistribution(MessageDistribution messageDistribution) {
        messageDistributionList.add(messageDistribution);
        if (isStarted()) {
            for (BrokerModel brokerModel : model.getBrokers()) {
                try {
                    brokerModel.createTransport(messageDistribution);
                } catch (Exception e) {
                    LOG.warn("Failed to create transport to " + brokerModel + " for " + messageDistribution);
                }
            }
        }
    }

    @Override
    public void removeMessageDistribution(MessageDistribution messageDistribution) {
        messageDistributionList.remove(messageDistribution);
        for (BrokerModel brokerModel : model.getBrokers()) {
            brokerModel.removeTransport(messageDistribution);
        }
    }

    @Override
    public void addBrokerModelChangedListener(BrokerModelChangedListener brokerModelChangedListener) {
        brokerModelChangedListeners.add(brokerModelChangedListener);
    }

    @Override
    public void removeBrokerModelChangedListener(BrokerModelChangedListener brokerModelChangedListener) {
        brokerModelChangedListeners.remove(brokerModelChangedListener);
    }

    @Override
    public void scaleDown() {
        //take destinations from least loaded
        BrokerModel leastLoaded = model.getLeastLoadedBroker();
        if (leastLoaded != null) {
            BrokerModel nextLeastLoaded = model.getNextLeastLoadedBroker(leastLoaded);
            if (nextLeastLoaded != null) {
                leastLoaded.getWriteLock();
                nextLeastLoaded.getWriteLock();
                try {
                    leastLoaded.getWriteLock();
                    nextLeastLoaded.getWriteLock();
                    if (model.copyDestinations(leastLoaded, nextLeastLoaded)) {
                        destroyBroker(leastLoaded);
                    } else {
                        LOG.error("Scale back failed");
                    }
                } finally {
                    leastLoaded.unlockWriteLock();
                    nextLeastLoaded.unlockWriteLock();
                }
            }
        }
    }

    @Override
    public void scaleUp() {
        createBroker();
    }

    @Override
    public void distributeLoad() {
        if (!scalingInProgress.isWorking()) {
            if (model.getBrokerCount() > 1) {
                final BrokerModel leastLoaded = model.getLeastLoadedBroker();
                final BrokerModel mostLoaded = model.getMostLoadedBroker();

                int toCopy = mostLoaded.getActiveDestinationCount() - leastLoaded.getActiveDestinationCount();
                if (toCopy > 0) {
                    toCopy = toCopy / 2;
                    if (toCopy > 0) {
                        List<ActiveMQDestination> copyList = model.getSortedDestinations(mostLoaded, toCopy);
                        //check to see we won't break limits
                        if (!copyList.isEmpty()) {
                            model.copyDestinations(mostLoaded, leastLoaded, copyList);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        if (poller != null) {
            poller.cancel(true);
        }
        scalingEngine.remove(this);
        serviceStopper.stop(scalingEngine);
        for (BrokerModel brokerModel : model.getBrokers()) {
            serviceStopper.stop(brokerModel);
        }
        serviceStopper.stop(brokerCoordinator);
    }

    @Override
    protected void doStart() throws Exception {
        setBrokerTemplateLocation(getBrokerTemplateLocation());

        model.start();
        scalingEngine.add(this);
        scalingEngine.start();

        setBrokerCoordinatorType(getBrokerCoordinatorType());
        brokerCoordinator = BrokerCoordinatorFactory.getCoordinator(brokerCoordinatorType);
        brokerCoordinator.start();
        pollBrokers();
        if (model.getBrokerCount() == 0) {
            createBroker();
        }
        Runnable run = new Runnable() {
            @Override
            public void run() {
                try {
                    scheduledTasks();

                } catch (Throwable e) {
                    LOG.error("Failed to validate MQ getLoad: ", e);
                }
            }
        };
        poller = asyncExecutors.scheduleAtFixedRate(run, getPollTime(), getPollTime() * 2);
        for (BrokerModel brokerModel : model.getBrokers()) {
            brokerModel.start();
        }
    }

    private void scheduledTasks() {
        int brokerCount = model.getBrokerCount();
        pollBrokers();
        scalingInProgress.finished(model.getBrokerCount());
        //check scaling Rules
        scalingEngine.process();
        if (brokerCount != model.getBrokerCount()) {
            for (BrokerModelChangedListener brokerModelChangedListener : brokerModelChangedListeners) {
                brokerModelChangedListener.brokerNumberChanged(model.getBrokerCount());
            }
        }
    }

}
