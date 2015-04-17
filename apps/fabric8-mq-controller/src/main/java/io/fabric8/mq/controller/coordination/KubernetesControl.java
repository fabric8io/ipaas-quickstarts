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

package io.fabric8.mq.controller.coordination;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.KubernetesFactory;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.jolokia.JolokiaClients;
import io.fabric8.mq.controller.MessageDistribution;
import io.fabric8.mq.controller.coordination.brokers.BrokerDestinationOverviewImpl;
import io.fabric8.mq.controller.coordination.brokers.BrokerModel;
import io.fabric8.mq.controller.coordination.brokers.BrokerOverview;
import io.fabric8.mq.controller.coordination.brokers.BrokerView;
import io.fabric8.mq.controller.util.Utils;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.jolokia.client.J4pClient;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pResponse;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Default;
import javax.management.ObjectName;
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

@Default
public class KubernetesControl extends BaseBrokerControl {
    static final int DEFAULT_POLLING_TIME = 2000;
    private static final Logger LOG = LoggerFactory.getLogger(KubernetesControl.class);

    private KubernetesClient kubernetes;
    private JolokiaClients clients;
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
                        LOG.info("Checking pod " + pod.getId() + " container: " + container.getName() + " image: " + container.getImage());
                        J4pClient client = clients.clientForContainer(host, container, pod);

                        populateBrokerStatistics(pod, client);

                    } catch (Throwable e) {
                        LOG.error("Failed to get broker statistics for pod:  " + pod.getId());
                    }
                }
            }

        } catch (Throwable e) {
            LOG.error("Failed to pollBrokers ", e);
        }
    }

    private void populateBrokerStatistics(Pod pod, J4pClient client) {
        ObjectName root = null;
        String attribute = "";
        if (client != null) {

            try {
                root = getBrokerJMXRoot(client);
                attribute = "BrokerName";
                Object brokerName = getAttribute(client, root, attribute);

                attribute = "BrokerId";
                Object brokerId = getAttribute(client, root, attribute);

                attribute = "OpenWireURL";
                Object uri = getAttribute(client, root, attribute);

                BrokerModel brokerModel = model.getBrokerById(brokerId.toString());
                if (brokerModel == null) {
                    BrokerView brokerView = new BrokerView();
                    brokerView.setBrokerName(brokerName.toString());
                    brokerView.setBrokerId(brokerId.toString());
                    brokerView.setUri(uri.toString());
                    brokerModel = new BrokerModel(pod, brokerView);
                    brokerModel.start();
                    model.add(brokerModel);
                    //add transports
                    for (MessageDistribution messageDistribution : messageDistributionList) {
                        brokerView.createTransport(messageDistribution);
                    }
                } else {
                    //transport might not be valid
                    brokerModel.setUri(uri.toString());
                    brokerModel.updateTransport();
                }

                BrokerOverview brokerOverview = new BrokerOverview();

                attribute = "TotalConnectionsCount";
                Number result = (Number) getAttribute(client, root, attribute);
                brokerOverview.setTotalConnections(result.intValue());
                populateDestinations(client, root, brokerOverview);
                brokerModel.setBrokerStatistics(brokerOverview);

            } catch (Throwable e) {
                LOG.error("Unable able to get BrokerStatistics from root=" + root + ",attribute: " + attribute, e);
            }
        }
    }

    private ObjectName getBrokerJMXRoot(J4pClient client) throws Exception {

        String type = "org.apache.activemq:*,type=Broker";
        String attribute = "BrokerName";
        ObjectName objectName = new ObjectName(type);
        J4pResponse<J4pReadRequest> result = client.execute(new J4pReadRequest(objectName, attribute));
        JSONObject jsonObject = result.getValue();
        return new ObjectName(jsonObject.keySet().iterator().next().toString());

    }

    private Object getAttribute(J4pClient client, ObjectName objectName, String attribute) throws Exception {
        J4pResponse<J4pReadRequest> result = client.execute(new J4pReadRequest(objectName, attribute));
        return result.getValue();
    }

    private BrokerOverview populateDestinations(J4pClient client, ObjectName root, BrokerOverview brokerOverview) throws Exception {
        populateDestinations(client, root, BrokerDestinationOverviewImpl.Type.QUEUE, brokerOverview);
        populateDestinations(client, root, BrokerDestinationOverviewImpl.Type.TOPIC, brokerOverview);
        return brokerOverview;
    }

    private BrokerOverview populateDestinations(J4pClient client, ObjectName root, BrokerDestinationOverviewImpl.Type type, BrokerOverview brokerOverview) {

        try {
            Hashtable<String, String> props = root.getKeyPropertyList();
            props.put("destinationType", type == BrokerDestinationOverviewImpl.Type.QUEUE ? "Queue" : "Topic");
            props.put("destinationName", "*");
            String objectName = root.getDomain() + ":" + Utils.getOrderedProperties(props);

            J4pResponse<J4pReadRequest> response = client.execute(new J4pReadRequest(objectName, "Name", "QueueSize", "ConsumerCount", "ProducerCount"));
            JSONObject value = response.getValue();
            for (Object key : value.keySet()) {
                //get the destinations
                JSONObject jsonObject = (JSONObject) value.get(key);
                String name = jsonObject.get("Name").toString();
                String producerCount = jsonObject.get("ProducerCount").toString().trim();
                String consumerCount = jsonObject.get("ConsumerCount").toString().trim();
                String queueSize = jsonObject.get("QueueSize").toString().trim();

                if (!name.contains("Advisory") && !name.contains(ActiveMQDestination.TEMP_DESTINATION_NAME_PREFIX)) {
                    ActiveMQDestination destination = type == BrokerDestinationOverviewImpl.Type.QUEUE ? new ActiveMQQueue(name) : new ActiveMQTopic(name);
                    BrokerDestinationOverviewImpl brokerDestinationOverviewImpl = new BrokerDestinationOverviewImpl(destination);
                    brokerDestinationOverviewImpl.setNumberOfConsumers(Integer.parseInt(consumerCount));
                    brokerDestinationOverviewImpl.setNumberOfProducers(Integer.parseInt(producerCount));
                    brokerDestinationOverviewImpl.setQueueDepth(Integer.parseInt(queueSize));
                    brokerOverview.addDestinationStatistics(brokerDestinationOverviewImpl);
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
        if (workInProgress.startWork(desiredNumber)) {
            try {
                String id = getOrCreateBrokerReplicationControllerId();
                ReplicationController replicationController = kubernetes.getReplicationController(id);
                int currentDesiredNumber = replicationController.getDesiredState().getReplicas();
                if (desiredNumber == (currentDesiredNumber + 1)) {
                    replicationController.getDesiredState().setReplicas(desiredNumber);
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
        if (workInProgress.startWork(desiredNumber)) {
            try {
                String id = getOrCreateBrokerReplicationControllerId();
                ReplicationController replicationController = kubernetes.getReplicationController(id);
                int currentDesiredNumber = replicationController.getDesiredState().getReplicas();
                if (desiredNumber == (currentDesiredNumber - 1)) {
                    replicationController.getDesiredState().setReplicas(desiredNumber);
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
                ObjectMapper mapper = KubernetesFactory.createObjectMapper();

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
                    replicationControllerId = replicationController.getId();
                    ReplicationController running = kubernetes.getReplicationController(replicationControllerId);
                    if (running == null) {
                        kubernetes.createReplicationController(replicationController);
                        LOG.info("Created ReplicationController " + replicationControllerId);
                    } else {
                        LOG.info("Found ReplicationController " + running.getId());
                        replicationControllerId = running.getId();
                    }

                } else {
                    LOG.error("Could not find location of Broker Template from " + getBrokerTemplateLocation());
                }

            } catch (Throwable e) {
                LOG.error("Failed to create a Broker", e);
            }
        }
        return replicationControllerId;
    }
}
