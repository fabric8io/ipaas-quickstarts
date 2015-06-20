/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.mq.autoscaler;

import io.fabric8.utils.Systems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MQAutoScalerMain {
    private static final Logger LOG = LoggerFactory.getLogger(MQAutoScalerMain.class);

    public static void main(String args[]) {

        try {
            MQAutoScaler mqAutoScaler = new MQAutoScaler();
            String brokerName = Systems.getEnvVarOrSystemProperty("AMQ_SERVICE_ID", mqAutoScaler.getBrokerName());
            mqAutoScaler.setBrokerName(brokerName);
            String groupName = Systems.getEnvVarOrSystemProperty("AMQ_GROUP_NAME", mqAutoScaler.getGroupName());
            mqAutoScaler.setGroupName(groupName);
            Number pollTime = Systems.getEnvVarOrSystemProperty("POLL_TIME", mqAutoScaler.getPollTime());
            mqAutoScaler.setPollTime(pollTime.intValue());
            Number maximumGroupSize = Systems.getEnvVarOrSystemProperty("MAX_GROUP_SIZE", mqAutoScaler.getMaximumGroupSize());
            mqAutoScaler.setMaximumGroupSize(maximumGroupSize.intValue());
            Number minimumGroupSize = Systems.getEnvVarOrSystemProperty("MIN_GROUP_SIZE", mqAutoScaler.getMinimumGroupSize());
            mqAutoScaler.setMinimumGroupSize(minimumGroupSize.intValue());
            Number maxBrokerConnections = Systems.getEnvVarOrSystemProperty("MAX_BROKER_CONNECTIONS", mqAutoScaler.getMaxConnectionsPerBroker());
            mqAutoScaler.setMaxConnectionsPerBroker(maxBrokerConnections.intValue());
            Number maxBrokerDestinations = Systems.getEnvVarOrSystemProperty("MAX_BROKER_DESTINATIONS", mqAutoScaler.getMaxDestinationsPerBroker());
            mqAutoScaler.setMaxDestinationsPerBroker(maxBrokerDestinations.intValue());
            Number maxDestinationDepth = Systems.getEnvVarOrSystemProperty("MAX_DESTINATION_DEPTH", mqAutoScaler.getMaxDestinationDepth());
            mqAutoScaler.setMaxDestinationDepth(maxDestinationDepth.intValue());
            Number maxProducersPerDestination = Systems.getEnvVarOrSystemProperty("MAX_PRODUCERS_PER_DESTINATION", mqAutoScaler.getMaxProducersPerDestination());
            mqAutoScaler.setMaxProducersPerDestination(maxProducersPerDestination.intValue());
            Number maxConsumersPerDestination = Systems.getEnvVarOrSystemProperty("MAX_CONSUMERS_PER_DESTINATION", mqAutoScaler.getMaxConsumersPerDestination());
            mqAutoScaler.setMaxConsumersPerDestination(maxConsumersPerDestination.intValue());
            Number minProducersPerDestination = Systems.getEnvVarOrSystemProperty("MIN_PRODUCERS_PER_DESTINATION", mqAutoScaler.getMinProducersPerDestination());
            mqAutoScaler.setMaxProducersPerDestination(minProducersPerDestination.intValue());
            Number minConsumersPerDestination = Systems.getEnvVarOrSystemProperty("MIN_CONSUMERS_PER_DESTINATION", mqAutoScaler.getMinConsumersPerDestination());
            mqAutoScaler.setMaxConsumersPerDestination(minConsumersPerDestination.intValue());
            mqAutoScaler.start();

            waiting();
        } catch (Throwable e) {
            LOG.error("Failed to start MQAutoScaler", e);
        }

    }

    static void waiting() {
        while (true) {
            final Object object = new Object();
            synchronized (object) {
                try {
                    object.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

}
