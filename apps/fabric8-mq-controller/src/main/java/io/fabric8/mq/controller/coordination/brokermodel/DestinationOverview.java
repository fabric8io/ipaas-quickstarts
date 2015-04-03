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

public class DestinationOverview implements Comparable<DestinationOverview> {
    private static final Logger LOG = LoggerFactory.getLogger(DestinationOverview.class);
    private final ActiveMQDestination destination;
    private int queueDepth;
    private int numberOfProducers;
    private int numberOfConsumers;
    private int queueDepthRate;

    public DestinationOverview(ActiveMQDestination destination) {
        this.destination = destination;
    }

    public ActiveMQDestination getDestination() {
        return destination;
    }

    public int getNumberOfConsumers() {
        return numberOfConsumers;
    }

    public void setNumberOfConsumers(int numberOfConsumers) {
        this.numberOfConsumers = numberOfConsumers;
    }

    public int getNumberOfProducers() {
        return numberOfProducers;
    }

    public void setNumberOfProducers(int numberOfProducers) {
        this.numberOfProducers = numberOfProducers;
    }

    public int getQueueDepth() {
        return queueDepth;
    }

    public void setQueueDepth(int queueDepth) {
        queueDepthRate = queueDepth - this.queueDepth;
        this.queueDepth = queueDepth;
    }

    public int getQueueDepthRate() {
        return queueDepthRate;
    }

    @Override
    public int compareTo(DestinationOverview other) {
        int result = other.getQueueDepth() - getQueueDepth();
        if (result == 0) {
            result = other.getNumberOfProducers() - getNumberOfProducers();
            if (result == 0) {
                result = other.getNumberOfConsumers() - getNumberOfConsumers();
            }
        }
        return result;
    }

    public String toString() {
        String result = destination.getPhysicalName() + ":,depth=" + getQueueDepth() + ",producers=" + getNumberOfProducers() + ",consumers=" + getNumberOfConsumers();
        return result;
    }

    public enum Type {QUEUE, TOPIC}

}
