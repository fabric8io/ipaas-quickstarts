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

package io.fabric8.mq.coordination.brokers;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.mq.MessageDistribution;
import io.fabric8.mq.model.Model;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.fabric8.kubernetes.api.KubernetesHelper.getName;

public class BrokerModel extends ServiceSupport implements BrokerModelMBean {
    private static Logger LOG = LoggerFactory.getLogger(BrokerModel.class);
    private final Pod pod;
    private final BrokerView brokerView;
    private final Model model;

    public BrokerModel(Pod pod, BrokerView brokerView, Model model) {
        this.pod = pod;
        this.brokerView = brokerView;
        this.model = model;
    }

    public Pod getPod() {
        return pod;
    }

    @Override
    public String getBrokerId() {
        return brokerView.getBrokerId();
    }

    @Override
    public String getBrokerName() {
        return brokerView.getBrokerName();
    }

    @Override
    public String getPodId() {
        String result = "";
        Pod pod = getPod();
        if (pod != null) {
            result = getName(pod);
        }
        return result;
    }

    @Override
    public int getTotalConnections() {
        int result = 0;
        BrokerOverview brokerOverview = getBrokerOverview();
        if (brokerOverview != null) {
            result = brokerOverview.getTotalConnections();
        }
        return result;
    }

    @Override
    public int getLoad() {
        return model.getLoad(this);
    }

    @Override
    public int getTotalDestinations() {
        int result = 0;
        BrokerOverview brokerOverview = getBrokerOverview();
        if (brokerOverview != null) {
            result = brokerOverview.getTotalDestinations();
        }
        return result;
    }

    @Override
    public int getTotalActiveDestinations() {
        int result = 0;
        BrokerOverview brokerOverview = getBrokerOverview();
        if (brokerOverview != null) {
            result = brokerOverview.getTotalActiveDestinations();
        }
        return result;
    }

    @Override
    public int getTotalActiveQueues() {
        int result = 0;
        BrokerOverview brokerOverview = getBrokerOverview();
        if (brokerOverview != null) {
            result = brokerOverview.getTotalActiveQueues();
        }
        return result;
    }

    @Override
    public int getTotalActiveTopics() {
        int result = 0;
        BrokerOverview brokerOverview = getBrokerOverview();
        if (brokerOverview != null) {
            result = brokerOverview.getTotalActiveTopics();
        }
        return result;
    }

    @Override
    public int getTotalQueueDepth() {
        int result = 0;
        BrokerOverview brokerOverview = getBrokerOverview();
        if (brokerOverview != null) {
            result = brokerOverview.getTotalQueueDepth();
        }
        return result;
    }

    @Override
    public int getTotalConsumerCount() {
        int result = 0;
        BrokerOverview brokerOverview = getBrokerOverview();
        if (brokerOverview != null) {
            result = brokerOverview.getTotalConsumerCount();
        }
        return result;
    }

    @Override
    public int getTotalProducerCount() {
        int result = 0;
        BrokerOverview brokerOverview = getBrokerOverview();
        if (brokerOverview != null) {
            result = brokerOverview.getTotalProducerCount();
        }
        return result;
    }

    public List<BrokerDestinationOverviewMBean> getQueues() {
        List<BrokerDestinationOverviewMBean> result = Collections.EMPTY_LIST;
        BrokerOverview brokerOverview = getBrokerOverview();
        if (brokerOverview != null) {
            result = new ArrayList<>(brokerOverview.getQueueOverviews().values());
        }
        return result;
    }

    public List<BrokerDestinationOverviewMBean> getTopics() {
        List<BrokerDestinationOverviewMBean> result = Collections.EMPTY_LIST;
        BrokerOverview brokerOverview = getBrokerOverview();
        if (brokerOverview != null) {
            result = new ArrayList<>(brokerOverview.getTopicOverviews().values());
        }
        return result;
    }

    public int hashCode() {
        return brokerView.getBrokerId().hashCode();
    }

    public boolean equals(Object object) {
        if (object instanceof BrokerModel) {
            BrokerModel other = (BrokerModel) object;
            String otherId = other.brokerView.getBrokerId();
            return brokerView.getBrokerId().equals(otherId);
        }
        return false;
    }

    public BrokerOverview getBrokerOverview() {
        return brokerView.getBrokerOverview();
    }

    public int getDepth(ActiveMQDestination destination) {
        return brokerView.getBrokerOverview().getQueueDepth(destination);
    }

    public int getNumberOfProducers(ActiveMQDestination destination) {
        return brokerView.getBrokerOverview().getNumberOfProducers(destination);
    }

    public int getNumberOfConsumers(ActiveMQDestination destination) {
        return brokerView.getBrokerOverview().getNumberOfConsumers(destination);
    }

    public Transport getTransport(MessageDistribution messageDistribution) {
        return brokerView.getTransport(messageDistribution);
    }

    public void createTransport(MessageDistribution messageDistribution) throws Exception {
        brokerView.createTransport(messageDistribution);
    }

    public void removeTransport(MessageDistribution messageDistribution) {
        brokerView.removeTransport(messageDistribution);
    }

    public Collection<MessageDistribution> detachTransport() {
        return brokerView.detachTransport();
    }

    public void attachTransport(Collection<MessageDistribution> messageDistributions) {
        brokerView.attachTransport(messageDistributions);
    }

    public Set<ActiveMQDestination> getActiveDestinations() {
        Set<ActiveMQDestination> result = new HashSet<>();
        BrokerOverview brokerOverview = getBrokerOverview();
        if (brokerOverview != null) {
            for (Map.Entry<ActiveMQDestination, BrokerDestinationOverviewMBean> entry : brokerOverview.getQueueOverviews().entrySet()) {

                if (entry.getValue().getQueueDepth() > 0 || entry.getValue().getNumberOfProducers() > 0) {
                    result.add(entry.getKey());
                }
            }
        }
        return result;
    }

    public int getActiveDestinationCount() {
        return getActiveDestinations().size();
    }

    public void updateTransport() throws Exception {
        brokerView.updateTransport();
    }

    public String getUri() {
        return brokerView.getUri();
    }

    public void setUri(String uri) {
        brokerView.setUri(uri);
    }

    public void setBrokerStatistics(BrokerOverview brokerOverview) {
        brokerView.setBrokerOverview(brokerOverview);
    }

    public void getReadLock() {
        brokerView.getReadLock();
    }

    public void unlockReadLock() {
        brokerView.unlockReadLock();
    }

    public void getWriteLock() {
        LOG.info("Getting write lock for  " + getBrokerName() + " " + brokerView.readLockCount() + ") ... " + Thread.currentThread());
        brokerView.getWriteLock();
        LOG.info("Aquired write lock for  " + getBrokerName() + " " + Thread.currentThread());
    }

    public void unlockWriteLock() {
        brokerView.unlockWriteLock();
        LOG.info(" Released write lock for  " + getBrokerName() + " " + Thread.currentThread());
    }

    @Override
    public String toString() {
        return "BrokerModel:" + brokerView.getBrokerName() + "[" + brokerView.getBrokerId() + "(" + getUri() + ")" + "]";
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        serviceStopper.stop(brokerView);
    }

    @Override
    protected void doStart() throws Exception {
        brokerView.start();
    }
}
