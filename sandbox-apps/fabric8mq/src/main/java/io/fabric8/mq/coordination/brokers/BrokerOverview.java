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

import org.apache.activemq.command.ActiveMQDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BrokerOverview implements Comparable<BrokerOverview> {
    private static final Logger LOG = LoggerFactory.getLogger(BrokerOverview.class);

    private final Map<ActiveMQDestination, BrokerDestinationOverviewMBean> queueOverviews = new ConcurrentHashMap<>();
    private final Map<ActiveMQDestination, BrokerDestinationOverviewMBean> topicOverviews = new ConcurrentHashMap<>();
    private int totalConnections;
    private boolean blockedProducers;

    public Map<ActiveMQDestination, BrokerDestinationOverviewMBean> getQueueOverviews() {
        return queueOverviews;
    }

    public Map<ActiveMQDestination, BrokerDestinationOverviewMBean> getTopicOverviews() {
        return topicOverviews;
    }

    public int getTotalConnections() {
        return totalConnections;
    }

    public void setTotalConnections(int totalConnections) {
        this.totalConnections = totalConnections;
    }

    public int getTotalDestinations() {
        return queueOverviews.size() + topicOverviews.size();
    }

    public int getTotalActiveDestinations() {
        return getTotalActiveTopics() + getTotalActiveQueues();
    }

    public int getTotalActiveQueues() {
        return getTotalActiveCount(queueOverviews.values());
    }

    public int getTotalActiveTopics() {
        return getTotalActiveCount(topicOverviews.values());
    }

    private int getTotalActiveCount(Collection<BrokerDestinationOverviewMBean> brokerDestinationOverviewImpls) {
        int result = 0;
        for (BrokerDestinationOverviewMBean brokerDestinationOverview : brokerDestinationOverviewImpls) {
            if (brokerDestinationOverview.getQueueDepth() > 0) {
            //if (brokerDestinationOverview.getQueueDepth() > 0 || brokerDestinationOverview.getNumberOfConsumers() > 0) {
                    result++;
            }
        }
        return result;
    }

    public boolean isBlockedProducers() {
        return blockedProducers;
    }

    public void setBlockedProducers(boolean blockedProducers) {
        this.blockedProducers = blockedProducers;
    }

    @Override
    public int compareTo(BrokerOverview other) {
        int result = other.getQueueOverviews().size() - getQueueOverviews().size();
        if (result == 0) {
            for (Map.Entry<ActiveMQDestination, BrokerDestinationOverviewMBean> entry : getQueueOverviews().entrySet()) {
                BrokerDestinationOverviewMBean brokerDestinationOverview = other.getQueueOverviews().get(entry.getKey());
                if (brokerDestinationOverview != null) {
                    result = entry.getValue().compareTo(brokerDestinationOverview);
                    if (result == 0) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    public int getTotalQueueDepth() {
        int result = 0;
        for (BrokerDestinationOverviewMBean brokerDestinationOverview : getQueueOverviews().values()) {
            result += brokerDestinationOverview.getQueueDepth();
        }
        return result;
    }

    public int getTotalConsumerCount() {
        int result = 0;
        for (BrokerDestinationOverviewMBean brokerDestinationOverview : getQueueOverviews().values()) {
            result += brokerDestinationOverview.getNumberOfConsumers();
        }
        return result;
    }

    public int getTotalProducerCount() {
        int result = 0;
        for (BrokerDestinationOverviewMBean brokerDestinationOverview : getQueueOverviews().values()) {
            result += brokerDestinationOverview.getNumberOfProducers();
        }
        return result;
    }

    public int getQueueDepth(ActiveMQDestination destination) {
        int result = 0;
        BrokerDestinationOverviewMBean brokerDestinationOverview = queueOverviews.get(destination);
        if (brokerDestinationOverview != null) {
            result = brokerDestinationOverview.getQueueDepth();
        }
        return result;
    }

    public int getNumberOfProducers(ActiveMQDestination destination) {
        int result = 0;
        BrokerDestinationOverviewMBean brokerDestinationOverview = queueOverviews.get(destination);
        if (brokerDestinationOverview != null) {
            result = brokerDestinationOverview.getNumberOfProducers();
        }
        return result;
    }

    public int getNumberOfConsumers(ActiveMQDestination destination) {
        int result = 0;
        BrokerDestinationOverviewMBean brokerDestinationOverview = queueOverviews.get(destination);
        if (brokerDestinationOverview != null) {
            result = brokerDestinationOverview.getNumberOfConsumers();
        }
        return result;
    }

    public void addDestinationStatistics(BrokerDestinationOverview brokerDestinationOverview) {
        if (brokerDestinationOverview.getDestination().isQueue()) {
            queueOverviews.put(brokerDestinationOverview.getDestination(), brokerDestinationOverview);
        } else {
            topicOverviews.put(brokerDestinationOverview.getDestination(), brokerDestinationOverview);
        }
    }

    public String toString() {
        String result = "BrokerStatistics: connections =" + getTotalConnections() + ",destinations=" +
                            getTotalDestinations() + ",blockedProducers=" + isBlockedProducers();
        if (!topicOverviews.isEmpty()) {
            result += System.lineSeparator();
            result += "\tTopics=";
            String separator = "";
            for (BrokerDestinationOverviewMBean brokerDestinationOverview : topicOverviews.values()) {
                result += separator;
                result += brokerDestinationOverview.toString();
                separator += ",";
            }
        }
        if (!queueOverviews.isEmpty()) {
            result += System.lineSeparator();
            result += "\tQueues=";
            String separator = "";
            for (BrokerDestinationOverviewMBean brokerDestinationOverview : queueOverviews.values()) {
                result += separator;
                result += brokerDestinationOverview.toString();
                separator += ",";
            }
        }
        return result;
    }
}
