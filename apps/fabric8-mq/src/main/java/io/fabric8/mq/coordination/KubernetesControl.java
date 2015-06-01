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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.KubernetesFactory;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerStatus;
import io.fabric8.kubernetes.jolokia.JolokiaClients;
import io.fabric8.mq.MessageDistribution;
import io.fabric8.mq.coordination.brokers.BrokerDestinationOverview;
import io.fabric8.mq.coordination.brokers.BrokerModel;
import io.fabric8.mq.coordination.brokers.BrokerOverview;
import io.fabric8.mq.coordination.brokers.BrokerView;
import io.fabric8.mq.util.BrokerJmxUtils;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.jolokia.client.J4pClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.management.ObjectName;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static io.fabric8.kubernetes.api.KubernetesHelper.getName;

public class KubernetesControl extends BaseBrokerControl {
    static final int DEFAULT_POLLING_TIME = 2000;
    private static final Logger LOG = LoggerFactory.getLogger(KubernetesControl.class);

    private KubernetesClient kubernetes;
    private JolokiaClients clients;
    @Inject
    @ConfigProperty(name = "AMQ_BROKER_CONTROLLER_ID", defaultValue = "amq-broker")
    private String defaultReplicationControllerId = "amq-broker";
    private String replicationControllerId;

    @Override
    protected void doStart() throws Exception {
        kubernetes = new KubernetesClient();

        clients = new JolokiaClients(kubernetes);
        //this will create the broker ReplicationController if it doesn't exist
        this.replicationControllerId = getOrCreateBrokerReplicationControllerId();
        super.doStart();
    }

    public void pollBrokers() {
        try {
            Map<String, Pod> podMap = KubernetesHelper.getSelectedPodMap(kubernetes, getBrokerSelector());
            Collection<Pod> pods = podMap.values();
            LOG.info("Checking " + getBrokerSelector() + ": groupSize = " + pods.size());
            for (Pod pod : pods) {
                String host = KubernetesHelper.getHost(pod);
                List<Container> containers = KubernetesHelper.getContainers(pod);

                for (Container container : containers) {
                    try {
                        LOG.info("Checking pod " + getName(pod) + " container: " + container.getName() + " image: " + container.getImage());
                        J4pClient client = clients.clientForContainer(host, container, pod);

                        populateBrokerStatistics(pod, container, client);

                    } catch (Throwable e) {
                        LOG.error("Failed to get broker statistics for pod:  " + getName(pod));
                    }
                }
            }

        } catch (Throwable e) {
            LOG.error("Failed to pollBrokers ", e);
        }
    }

    private void populateBrokerStatistics(Pod pod, Container container,J4pClient client) {
        ObjectName root = null;
        String attribute = "";
        if (client != null) {

            try {
                root = BrokerJmxUtils.getRoot(client);
                attribute = "BrokerName";
                Object brokerName = BrokerJmxUtils.getAttribute(client, root, attribute);

                attribute = "BrokerId";
                Object brokerId = BrokerJmxUtils.getAttribute(client, root, attribute);

                attribute = "OpenWireURL";
                Object uriObj = BrokerJmxUtils.getAttribute(client, root, attribute);
                URI uri = new URI(uriObj.toString());
                int port = uri.getPort();

                String amqBrokerURI = "tcp://" + pod.getStatus().getPodIP() + ":" + port;

                BrokerModel brokerModel = model.getBrokerById(brokerId.toString());
                if (brokerModel == null) {
                    BrokerView brokerView = new BrokerView();
                    brokerView.setBrokerName(brokerName.toString());
                    brokerView.setBrokerId(brokerId.toString());
                    brokerView.setUri(amqBrokerURI);
                    brokerModel = new BrokerModel(pod, brokerView, model);
                    brokerModel.start();
                    model.add(brokerModel);
                    //add transports
                    for (MessageDistribution messageDistribution : messageDistributionList) {
                        brokerView.createTransport(messageDistribution);
                    }
                } else {
                    //transport might not be valid
                    brokerModel.setUri(amqBrokerURI);
                    brokerModel.updateTransport();
                }

                BrokerOverview brokerOverview = new BrokerOverview();

                attribute = "TotalConnectionsCount";
                Number result = (Number) BrokerJmxUtils.getAttribute(client, root, attribute);
                brokerOverview.setTotalConnections(result.intValue());
                populateDestinations(client, root, brokerOverview);
                brokerModel.setBrokerStatistics(brokerOverview);

            } catch (Throwable e) {
                LOG.error("Unable able to get BrokerStatistics from root=" + root + ",attribute: " + attribute, e);
            }
        }
    }


    private BrokerOverview populateDestinations(J4pClient client, ObjectName root, BrokerOverview brokerOverview) throws Exception {
        populateDestinations(client, root, BrokerDestinationOverview.Type.QUEUE, brokerOverview);
        populateDestinations(client, root, BrokerDestinationOverview.Type.TOPIC, brokerOverview);
        return brokerOverview;
    }

    private BrokerOverview populateDestinations(J4pClient client, ObjectName root, BrokerDestinationOverview.Type type, BrokerOverview brokerOverview) {
        try {
            String typeName = type == BrokerDestinationOverview.Type.QUEUE ? "Queue" : "Topic";
            List<ObjectName> list = BrokerJmxUtils.getDestinations(client, root, typeName);

            for (ObjectName objectName : list) {
                String destinationName = objectName.getKeyProperty("destinationName");
                if (destinationName.toLowerCase().contains("advisory") && !destinationName.contains(ActiveMQDestination.TEMP_DESTINATION_NAME_PREFIX)) {
                    String producerCount = BrokerJmxUtils.getAttribute(client, objectName, "ProducerCount").toString().trim();
                    String consumerCount = BrokerJmxUtils.getAttribute(client, objectName, "ConsumerCount").toString().trim();
                    String queueSize = BrokerJmxUtils.getAttribute(client, objectName, "QueueSize").toString().trim();
                    ActiveMQDestination destination = type == BrokerDestinationOverview.Type.QUEUE ? new ActiveMQQueue(destinationName) : new ActiveMQTopic(destinationName);
                    BrokerDestinationOverview brokerDestinationOverview = new BrokerDestinationOverview(destination);
                    brokerDestinationOverview.setNumberOfConsumers(Integer.parseInt(consumerCount));
                    brokerDestinationOverview.setNumberOfProducers(Integer.parseInt(producerCount));
                    brokerDestinationOverview.setQueueDepth(Integer.parseInt(queueSize));
                    brokerOverview.addDestinationStatistics(brokerDestinationOverview);
                }
            }
        } catch (Exception ex) {
            // Destinations don't exist yet on the broker
            LOG.debug("populateDestinations failed", ex);
        }
        return brokerOverview;
    }

    public void createBroker() {
        int desiredNumber = model.getBrokerCount() + 1;
        if (scalingInProgress.startWork(desiredNumber)) {
            try {
                String id = getOrCreateBrokerReplicationControllerId();
                ReplicationController replicationController = kubernetes.getReplicationController(id);
                ReplicationControllerStatus status = replicationController.getStatus();
                int currentDesiredNumber = 0;
                if (status != null) {
                    currentDesiredNumber = status.getReplicas();
                } else {
                    status = new ReplicationControllerStatus();
                    replicationController.setStatus(status);
                }
                if (desiredNumber == (currentDesiredNumber + 1)) {
                    replicationController.getStatus().setReplicas(desiredNumber);
                    kubernetes.updateReplicationController(id, replicationController);
                    LOG.error("Updated Broker Replication Controller desired state from " + currentDesiredNumber + " to " + desiredNumber);
                }
            } catch (Throwable e) {
                LOG.error("Failed to create a Broker", e);
            }
        }
    }

    public void destroyBroker(BrokerModel brokerModel) {
        int desiredNumber = model.getBrokerCount() - 1;
        if (scalingInProgress.startWork(desiredNumber)) {
            try {
                String id = getOrCreateBrokerReplicationControllerId();
                ReplicationController replicationController = kubernetes.getReplicationController(id);
                int currentDesiredNumber = replicationController.getStatus().getReplicas();
                if (desiredNumber == (currentDesiredNumber - 1)) {
                    replicationController.getStatus().setReplicas(desiredNumber);
                    model.remove(brokerModel);
                    //Todo update when Kubernetes allows you to target exact pod to discard from replication controller
                    kubernetes.deletePod(brokerModel.getPod());
                    kubernetes.updateReplicationController(id, replicationController);
                    LOG.error("Updated Broker Replication Controller desired state from " + currentDesiredNumber + " to " + desiredNumber + " and removed Broker " + brokerModel);
                }
            } catch (Throwable e) {
                LOG.error("Failed to create a Broker", e);
            }
        }
    }

    private String getOrCreateBrokerReplicationControllerId() {
        if (replicationControllerId == null) {
            try {
                ReplicationController running = kubernetes.getReplicationController(defaultReplicationControllerId);
                if (running == null) {
                    ObjectMapper mapper = KubernetesFactory.createObjectMapper();

                    //ToDo chould change this to look for ReplicationController for AMQ_Broker from Maven
                    File file = new File(getBrokerTemplateLocation());
                    URL url;
                    if (file.exists()) {
                        url = Paths.get(file.getAbsolutePath()).toUri().toURL();
                    } else {
                        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                        url = classLoader.getResource(getBrokerTemplateLocation());
                    }

                    if (url != null) {
                        ReplicationController replicationController = mapper.reader(ReplicationController.class).readValue(url);
                        replicationControllerId = getName(replicationController);
                        running = kubernetes.getReplicationController(replicationControllerId);
                        if (running == null) {
                            kubernetes.createReplicationController(replicationController);
                            LOG.info("Created ReplicationController " + replicationControllerId);
                        } else {
                            replicationControllerId = getName(running);
                            LOG.info("Found ReplicationController " + replicationControllerId);
                        }

                    } else {
                        LOG.error("Could not find location of Broker Template from " + getBrokerTemplateLocation());
                    }
                }else{
                    replicationControllerId = defaultReplicationControllerId;
                }

            } catch (Throwable e) {
                LOG.error("Failed to create a Broker", e);
            }
        }
        return replicationControllerId;
    }
}
