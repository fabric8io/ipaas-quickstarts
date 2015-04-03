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
package io.fabric8.mq.controller.coordination.brokermodel;

import org.apache.activemq.command.ActiveMQDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BrokerOverview implements Comparable<BrokerOverview> {
    private static final Logger LOG = LoggerFactory.getLogger(BrokerOverview.class);

    private final String brokerId;
    private final String brokerName;
    private final Map<ActiveMQDestination, DestinationOverview> queueOverviews = new ConcurrentHashMap<>();
    private final Map<ActiveMQDestination, DestinationOverview> topicOverviews = new ConcurrentHashMap<>();
    private int totalConnections;
    private boolean blockedProducers;

    public BrokerOverview(String brokerId, String brokerName) {
        this.brokerId = brokerId;
        this.brokerName = brokerName;
    }

    public Map<ActiveMQDestination, DestinationOverview> getQueueOverviews() {
        return queueOverviews;
    }

    public Map<ActiveMQDestination, DestinationOverview> getTopicOverviews() {
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
            for (Map.Entry<ActiveMQDestination, DestinationOverview> entry : getQueueOverviews().entrySet()) {
                DestinationOverview destinationOverview = other.getQueueOverviews().get(entry.getKey());
                if (destinationOverview != null) {
                    result = entry.getValue().compareTo(destinationOverview);
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
        for (DestinationOverview destinationOverview : getQueueOverviews().values()) {
            result += destinationOverview.getQueueDepth();
        }
        return result;
    }

    public int getTotalConsumerCount() {
        int result = 0;
        for (DestinationOverview destinationOverview : getQueueOverviews().values()) {
            result += destinationOverview.getNumberOfConsumers();
        }
        return result;
    }

    public int getTotalProducerCount() {
        int result = 0;
        for (DestinationOverview destinationOverview : getQueueOverviews().values()) {
            result += destinationOverview.getNumberOfProducers();
        }
        return result;
    }

    public int getQueueDepth(ActiveMQDestination destination) {
        int result = 0;
        DestinationOverview destinationOverview = queueOverviews.get(destination);
        if (destinationOverview != null) {
            result = destinationOverview.getQueueDepth();
        }
        return result;
    }

    public int getNumberOfProducers(ActiveMQDestination destination) {
        int result = 0;
        DestinationOverview destinationOverview = queueOverviews.get(destination);
        if (destinationOverview != null) {
            result = destinationOverview.getNumberOfProducers();
        }
        return result;
    }

    public int getNumberOfConsumers(ActiveMQDestination destination) {
        int result = 0;
        DestinationOverview destinationOverview = queueOverviews.get(destination);
        if (destinationOverview != null) {
            result = destinationOverview.getNumberOfConsumers();
        }
        return result;
    }

    public void addDestinationStatistics(DestinationOverview destinationOverview) {
        if (destinationOverview.getDestination().isQueue()) {
            queueOverviews.put(destinationOverview.getDestination(), destinationOverview);
        } else {
            topicOverviews.put(destinationOverview.getDestination(), destinationOverview);
        }
    }

    public String getBrokerIdentifier() {
        return brokerName + "[" + brokerId + "]";
    }

    public String toString() {
        String result = "BrokerStatistics(" + getBrokerIdentifier() + ") connections=" + getTotalConnections() + ",destinations=" +
                            getTotalDestinations() + ",blockedProducers=" + isBlockedProducers();
        if (!topicOverviews.isEmpty()) {
            result += System.lineSeparator();
            result += "\tTopics=";
            String separator = "";
            for (DestinationOverview destinationOverview : topicOverviews.values()) {
                result += separator;
                result += destinationOverview.toString();
                separator += ",";
            }
        }
        if (!queueOverviews.isEmpty()) {
            result += System.lineSeparator();
            result += "\tQueues=";
            String separator = "";
            for (DestinationOverview destinationOverview : queueOverviews.values()) {
                result += separator;
                result += destinationOverview.toString();
                separator += ",";
            }
        }
        return result;
    }
}
