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

import org.apache.deltaspike.core.api.config.ConfigProperty;

import javax.inject.Inject;

public class DefaultBrokerLimitsConfig implements BrokerLimitsConfig {
    @Inject
    @ConfigProperty(name = "MAX_CONNECTIONS_PER_BROKER", defaultValue = "100")
    private int maxConnectionsPerBroker = 100;
    @Inject
    @ConfigProperty(name = "MAX_DESTINATIONS_PER_BROKER", defaultValue = "25")
    private int maxDestinationsPerBroker = 25;
    @Inject
    @ConfigProperty(name = "MAX_DESTINATION_DEPTH", defaultValue = "20")
    private int maxDestinationDepth = 20;
    @Inject
    @ConfigProperty(name = "MAX_PRODUCERS_PER_DESTINATION", defaultValue = "10")
    private int maxProducersPerDestination = 10;

    @Inject
    @ConfigProperty(name = "MAX_CONSUMERS_PER_DESTINATION", defaultValue = "10")
    private int maxConsumersPerDestination = 10;

    @Inject
    @ConfigProperty(name = "MAX_NUMBER_OF_BROKERS", defaultValue = "20")
    private int maxNumberOfBrokers = 20;

    public int getMaxConsumersPerDestination() {
        return maxConsumersPerDestination;
    }

    public void setMaxConsumersPerDestination(int maxConsumersPerDestination) {
        this.maxConsumersPerDestination = maxConsumersPerDestination;
    }

    public int getMaxConnectionsPerBroker() {
        return maxConnectionsPerBroker;
    }

    public void setMaxConnectionsPerBroker(int maxConnectionsPerBroker) {
        this.maxConnectionsPerBroker = maxConnectionsPerBroker;
    }

    public int getMaxDestinationsPerBroker() {
        return maxDestinationsPerBroker;
    }

    public void setMaxDestinationsPerBroker(int maxDestinationsPerBroker) {
        this.maxDestinationsPerBroker = maxDestinationsPerBroker;
    }

    public int getMaxDestinationDepth() {
        return maxDestinationDepth;
    }

    public void setMaxDestinationDepth(int maxDestinationDepth) {
        this.maxDestinationDepth = maxDestinationDepth;
    }

    public int getMaxProducersPerDestination() {
        return maxProducersPerDestination;
    }

    public void setMaxProducersPerDestination(int maxProducersPerDestination) {
        this.maxProducersPerDestination = maxProducersPerDestination;
    }

    @Override
    public int getMaxNumberOfBrokers() {
        return maxNumberOfBrokers;
    }

    public void setMaxNumberOfBrokers(int maxNumberOfBrokers) {
        this.maxNumberOfBrokers = maxNumberOfBrokers;
    }
}
