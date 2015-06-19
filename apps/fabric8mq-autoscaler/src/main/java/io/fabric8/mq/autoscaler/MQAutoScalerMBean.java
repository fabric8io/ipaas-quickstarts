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

public interface MQAutoScalerMBean {

    int getMaxConnectionsPerBroker();

    void setMaxConnectionsPerBroker(int maxConnectionsPerBroker);

    int getMaxDestinationsPerBroker();

    void setMaxDestinationsPerBroker(int maxDestinationsPerBroker);

    int getMaxConsumersPerDestination();

    void setMaxConsumersPerDestination(int maxConsumersPerDestination);

    int getMaxProducersPerDestination();

    void setMaxProducersPerDestination(int maxProducersPerDestination);

    int getMinConsumersPerDestination();

    void setMinConsumersPerDestination(int minConsumersPerDestination);

    int getMinProducersPerDestination();

    void setMinProducersPerDestination(int minProducersPerDestination);

    int getMaxDestinationDepth();

    void setMaxDestinationDepth(int maxDestinationDepth);

    String getGroupName();

    void setGroupName(String groupName);

    int getPollTime();

    void setPollTime(int pollTime);

    String getBrokerName();

    void setBrokerName(String brokerName);

    int getMaximumGroupSize();

    void setMaximumGroupSize(int maximumGroupSize);

    int getMinimumGroupSize();

    void setMinimumGroupSize(int minimumGroupSize);

}
