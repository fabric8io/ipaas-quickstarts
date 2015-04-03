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

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.mq.controller.sharding.MessageDistribution;
import io.fabric8.utils.Systems;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class BrokerModel extends ServiceSupport implements Comparable<BrokerModel> {
    private static Logger LOG = LoggerFactory.getLogger(BrokerModel.class);
    private final Pod pod;
    private final BrokerView brokerView;
    private int maxConnectionsPerBroker;
    private int maxDestinationsPerBroker;
    private int maxDestinationDepth;
    private int maxProducersPerDestination;
    private int maxConsumersPerDestination;

    public BrokerModel(Pod pod, BrokerView brokerView) {
        this.pod = pod;
        this.brokerView = brokerView;
        maxConnectionsPerBroker = 20;
        maxDestinationsPerBroker = 20;
        maxDestinationDepth = 20;
        maxProducersPerDestination = 20;
        maxConsumersPerDestination = 20;
    }

    public Pod getPod() {
        return pod;
    }

    public String getBrokerId() {
        return brokerView.getBrokerId();
    }

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

    public boolean areBrokerLimitsExceeded() {
        String brokerName = brokerView.getBrokerName();
        int totalConnections = brokerView.getBrokerOverview().getTotalConnections();
        boolean connectionsExceeded = totalConnections > getMaxConnectionsPerBroker();
        if (connectionsExceeded) {
            LOG.info("Broker " + brokerName + " EXCEEDED connection limits(" + getMaxConnectionsPerBroker() + ") with " + totalConnections + " connections");
        } else {
            LOG.info("Broker " + brokerName + " within connection limits(" + getMaxConnectionsPerBroker() + ") with " + totalConnections + " connections");
        }

        int totalDestinations = brokerView.getBrokerOverview().getTotalDestinations();
        boolean destinationsExceeded = totalDestinations > getMaxDestinationsPerBroker();

        if (destinationsExceeded) {
            LOG.info("Broker " + brokerName + " EXCEEDED destination limits(" + getMaxDestinationsPerBroker() + ") with " + totalDestinations + " destinations");
        } else {
            LOG.info("Broker " + brokerName + " within destination limits(" + getMaxDestinationsPerBroker() + ") with " + totalDestinations + " destinations");
        }
        return connectionsExceeded || destinationsExceeded;
    }

    public boolean areDestinationLimitsExceeded() {
        BrokerOverview brokerOverview = brokerView.getBrokerOverview();
        for (DestinationOverview destinationOverview : brokerOverview.getQueueOverviews().values()) {
            if (destinationOverview.getQueueDepth() > getMaxDestinationDepth()) {
                return true;
            }
            if (destinationOverview.getNumberOfProducers() > getMaxProducersPerDestination()) {
                return true;
            }
            if (destinationOverview.getNumberOfConsumers() > getMaxConsumersPerDestination()) {
                return true;
            }
        }
        return false;
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

    @Override
    public int compareTo(BrokerModel other) {
        int result = 0;
        if (this != other) {
            result = other.brokerView.getBrokerOverview().getTotalQueueDepth() - brokerView.getBrokerOverview().getTotalQueueDepth();
            if (result == 0) {
                result = other.brokerView.getBrokerOverview().getTotalDestinations() - brokerView.getBrokerOverview().getTotalDestinations();
            }
        }
        return result;
    }

    public int load() {
        int numConnections = brokerView.getBrokerOverview().getTotalConnections();
        int connectionLoad = (numConnections * 80) / getMaxConnectionsPerBroker();
        int numDestinations = brokerView.getDestinations().size();
        int destinationLoad = (numConnections * 20) / getMaxDestinationsPerBroker();
        return connectionLoad + destinationLoad;
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

    public Set<ActiveMQDestination> getQueues() {
        return brokerView.getQueues();
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

    public void lock() {
        brokerView.lock();
    }

    public void unlock() {
        brokerView.unlock();
    }

    public boolean isGcCandidate() {
        boolean result = false;
        return result;
    }

    public void gcSweep() {

    }

    public boolean doGc() {
        boolean result = false;
        return result;
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        serviceStopper.stop(brokerView);
    }

    @Override
    protected void doStart() throws Exception {
        Number maxBrokerConnections = Systems.getEnvVarOrSystemProperty("MAX_BROKER_CONNECTIONS", getMaxConnectionsPerBroker());
        setMaxConnectionsPerBroker(maxBrokerConnections.intValue());
        Number maxBrokerDestinations = Systems.getEnvVarOrSystemProperty("MAX_BROKER_DESTINATIONS", getMaxDestinationsPerBroker());
        setMaxDestinationsPerBroker(maxBrokerDestinations.intValue());
        Number maxDestinationDepth = Systems.getEnvVarOrSystemProperty("MAX_DESTINATION_DEPTH", getMaxDestinationDepth());
        setMaxDestinationDepth(maxDestinationDepth.intValue());
        Number maxProducersPerDestination = Systems.getEnvVarOrSystemProperty("MAX_PRODUCERS_PER_DESTINATION", getMaxProducersPerDestination());
        setMaxProducersPerDestination(maxProducersPerDestination.intValue());
        Number maxConsumersPerDestination = Systems.getEnvVarOrSystemProperty("MAX_CONSUMERS_PER_DESTINATION", getMaxConsumersPerDestination());
        setMaxConsumersPerDestination(maxConsumersPerDestination.intValue());
        brokerView.start();
    }
}
