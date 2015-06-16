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

import io.fabric8.mq.MessageDistribution;
import io.fabric8.mq.coordination.brokers.BrokerDestinationOverview;
import io.fabric8.mq.coordination.brokers.BrokerModel;
import io.fabric8.mq.coordination.brokers.BrokerOverview;
import io.fabric8.mq.coordination.brokers.BrokerView;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.broker.region.Destination;
import org.apache.activemq.command.ActiveMQDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Alternative;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Alternative
public class BrokerControlTestImpl extends BaseBrokerControl {
    private static final Logger LOG = LoggerFactory.getLogger(BrokerControlTestImpl.class);
    private List<BrokerService> brokers = new CopyOnWriteArrayList<>();

    public void pollBrokers() {
        try {
            for (BrokerService broker : brokers) {
                populateBrokerStatistics(broker);
            }

        } catch (Throwable e) {
            LOG.error("Failed to pollBrokers ", e);
        }
    }

    public void createBroker() {
        int desiredNumber = model.getBrokerCount() + 1;
        if (scalingInProgress.startWork(desiredNumber)) {
            try {
                String brokerName = "Broker-" + model.getBrokerCount();
                BrokerService broker = new BrokerService();
                broker.setBrokerName(brokerName);
                broker.setBrokerId(brokerName);
                broker.setDeleteAllMessagesOnStartup(true);
                broker.setPersistent(false);
                broker.setUseJmx(false);
                broker.setEnableStatistics(true);
                broker.addConnector("tcp://localhost:0");
                broker.start();
                brokers.add(broker);
                System.err.println("CREATED BROKER " + brokerName);
            } catch (Throwable e) {
                LOG.error("Failed to create a Broker", e);
            }
        }
    }

    public void destroyBroker(BrokerModel brokerModel) {
        int desiredNumber = model.getBrokerCount() - 1;
        if (scalingInProgress.startWork(desiredNumber)) {
            try {

                model.remove(brokerModel);
                for (BrokerService broker : brokers) {
                    if (broker.getBroker().getBrokerId().toString().equals(brokerModel.getBrokerId())) {
                        brokers.remove(broker);
                        broker.stop();
                        LOG.info("Destroyed Broker  " + brokerModel.getBrokerId());
                    }
                }

            } catch (Throwable e) {
                LOG.error("Failed to create a Broker", e);
            }
        }
    }

    private void populateBrokerStatistics(BrokerService broker) {
        try {
            String brokerId = broker.getBroker().getBrokerId().toString();
            BrokerModel brokerModel = model.getBrokerById(brokerId);
            if (brokerModel == null) {
                BrokerView brokerView = new BrokerView();
                brokerView.setBrokerName(broker.getBrokerName());
                brokerView.setBrokerId(brokerId);
                brokerView.setUri(broker.getDefaultSocketURIString());
                brokerModel = new BrokerModel(null, brokerView, model);
                brokerModel.start();
                model.add(brokerModel);
                //add transports
                for (MessageDistribution messageDistribution : messageDistributionList) {
                    brokerView.createTransport(messageDistribution);
                }
            } else {
                //transport might not be valid
                brokerModel.setUri(broker.getDefaultSocketURIString());
                brokerModel.updateTransport();
            }

            BrokerOverview brokerOverview = new BrokerOverview();
            int connections = 0;
            for (TransportConnector transportConnector : broker.getTransportConnectors()) {
                connections += transportConnector.getConnections().size();
            }
            brokerOverview.setTotalConnections(connections);
            populateDestinations(broker, brokerOverview);
            brokerModel.setBrokerStatistics(brokerOverview);

        } catch (Throwable e) {
            LOG.error("Unable able to get BrokerStatistics from " + broker, e);
        }

    }

    private BrokerOverview populateDestinations(BrokerService broker, BrokerOverview brokerOverview) throws Exception {
        populateDestinations(broker, BrokerDestinationOverview.Type.QUEUE, brokerOverview);
        populateDestinations(broker, BrokerDestinationOverview.Type.TOPIC, brokerOverview);
        return brokerOverview;
    }

    private BrokerOverview populateDestinations(BrokerService broker, BrokerDestinationOverview.Type type, BrokerOverview brokerOverview) {

        try {
            Map<ActiveMQDestination, Destination> map = broker.getRegionBroker().getDestinationMap();
            if (map != null) {
                for (Map.Entry<ActiveMQDestination, Destination> entry : map.entrySet()) {
                    ActiveMQDestination activeMQDestination = entry.getKey();
                    Destination destination = entry.getValue();
                    if (destination != null) {
                        String name = activeMQDestination.getPhysicalName();
                        if (!name.contains("Advisory") && !name.contains(ActiveMQDestination.TEMP_DESTINATION_NAME_PREFIX)) {

                            BrokerDestinationOverview brokerDestinationOverview = new BrokerDestinationOverview(activeMQDestination);
                            brokerDestinationOverview.setNumberOfConsumers(destination.getConsumers().size());
                            brokerDestinationOverview.setNumberOfProducers((int) destination.getDestinationStatistics().getProducers().getCount());
                            brokerDestinationOverview.setQueueDepth((int) destination.getDestinationStatistics().getMessages().getCount());
                            brokerOverview.addDestinationStatistics(brokerDestinationOverview);
                        }
                    }

                }
            }
        } catch (Exception ex) {
            // Destinations don't exist yet on the broker
            LOG.debug("populateDestinations failed", ex);
        }
        return brokerOverview;
    }
}
