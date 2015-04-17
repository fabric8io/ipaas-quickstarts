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

package io.fabric8.mq.controller.coordination.brokers;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.mq.controller.MessageDistribution;
import io.fabric8.mq.controller.model.BrokerDestinationOverview;
import io.fabric8.mq.controller.model.BrokerStatistics;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BrokerModel extends ServiceSupport implements BrokerStatistics {
    private static Logger LOG = LoggerFactory.getLogger(BrokerModel.class);
    private final Pod pod;
    private final BrokerView brokerView;

    public BrokerModel(Pod pod, BrokerView brokerView) {
        this.pod = pod;
        this.brokerView = brokerView;
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
            result = pod.getId();
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
    public List<BrokerDestinationOverview> getQueues() {
        List<BrokerDestinationOverview> result = Collections.EMPTY_LIST;
        BrokerOverview brokerOverview = getBrokerOverview();
        if (brokerOverview != null) {
            result = new ArrayList(brokerOverview.getQueueOverviews().values());
        }
        return result;
    }

    @Override
    public List<BrokerDestinationOverview> getTopics() {
        List<BrokerDestinationOverview> result = Collections.EMPTY_LIST;
        BrokerOverview brokerOverview = getBrokerOverview();
        if (brokerOverview != null) {
            result = new ArrayList(brokerOverview.getTopicOverviews().values());
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

    public Set<ActiveMQDestination> getActiveDestinations() {
        Set result = new HashSet();
        BrokerOverview brokerOverview = getBrokerOverview();
        if (brokerOverview != null) {
            for (Map.Entry<ActiveMQDestination, BrokerDestinationOverview> entry : brokerOverview.getQueueOverviews().entrySet()) {

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
        brokerView.getWriteLock();
    }

    public void unlockWriteLock() {
        brokerView.unlockWriteLock();
    }

    @Override
    public String toString() {
        String str = "BrokerModel:" + brokerView.getBrokerName() + "[" + brokerView.getBrokerId() + "(" + getUri() + ")" + "]";
        return str;
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
