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
import io.fabric8.mq.controller.BrokerStateInfo;
import io.fabric8.mq.controller.coordination.brokermodel.BrokerModel;
import io.fabric8.mq.controller.coordination.brokermodel.BrokerOverview;
import io.fabric8.mq.controller.coordination.brokermodel.BrokerView;
import io.fabric8.mq.controller.coordination.brokermodel.DestinationOverview;
import io.fabric8.mq.controller.sharding.MessageDistribution;
import io.fabric8.mq.controller.util.CopyDestinationWorker;
import io.fabric8.mq.controller.util.Utils;
import io.fabric8.utils.Systems;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.filter.DestinationMap;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.jolokia.client.J4pClient;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pResponse;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class KubernetesControl extends ServiceSupport implements BrokerControl {
    static final int DEFAULT_POLLING_TIME = 2000;
    private static final Logger LOG = LoggerFactory.getLogger(KubernetesControl.class);
    private final BrokerStateInfo brokerStateInfo;
    private final DestinationMap destinationMap;
    private final List<MessageDistribution> messageDistributionList;
    private final WorkInProgress scalingInProgress;
    private Map<String, BrokerModel> brokerModelMap = new ConcurrentHashMap<>();
    private KubernetesClient kubernetes;
    private int pollTime;
    private JolokiaClients clients;
    private ScheduledFuture poller;
    private String brokerName;
    private String groupName;
    private String brokerTemplateLocation;
    private String brokerSelector;
    private BrokerCoordinator brokerCoordinator;
    private String brokerCoordinatorType;
    private String replicationControllerId;

    public KubernetesControl(BrokerStateInfo brokerStateInfo) {
        this.brokerStateInfo = brokerStateInfo;
        pollTime = DEFAULT_POLLING_TIME;
        brokerSelector = "container=java,component=fabric8MQ,provider=fabric8,group=fabric8MQGroup";
        brokerName = "fabric8MQ";
        groupName = "mqGroup";
        brokerTemplateLocation = "META-INF/replicator-template.json";
        brokerCoordinatorType = "singleton";
        destinationMap = new DestinationMap();
        messageDistributionList = new CopyOnWriteArrayList();
        scalingInProgress = new WorkInProgress();
    }

    public String getBrokerSelector() {
        return brokerSelector;
    }

    public void setBrokerSelector(String brokerSelector) {
        this.brokerSelector = brokerSelector;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public int getPollTime() {
        return pollTime;
    }

    public void setPollTime(int pollTime) {
        this.pollTime = pollTime;
    }

    public String getBrokerTemplateLocation() {
        return brokerTemplateLocation;
    }

    public void setBrokerTemplateLocation(String brokerTemplateLocation) {
        this.brokerTemplateLocation = brokerTemplateLocation;
    }

    public String getBrokerCoordinatorType() {
        return brokerCoordinatorType;
    }

    public void setBrokerCoordinatorType(String brokerCoordinatorType) {
        this.brokerCoordinatorType = brokerCoordinatorType;
    }

    @Override
    public Collection<Transport> getTransports(MessageDistribution messageDistribution) {
        List<Transport> list = new ArrayList<>();
        for (BrokerModel brokerModel : brokerModelMap.values()) {
            list.add(brokerModel.getTransport(messageDistribution));
        }
        return list;
    }

    @Override
    public Transport getTransport(MessageDistribution messageDistribution, ActiveMQDestination destination) {
        Set<BrokerModel> set = destinationMap.get(destination);
        if (set != null && !set.isEmpty()) {
            BrokerModel brokerModel = set.iterator().next();
            return brokerModel.getTransport(messageDistribution);
        } else {
            //allocate a broker for the destination
            List<BrokerModel> list = new ArrayList<>(brokerModelMap.values());
            Collections.sort(list);
            BrokerModel brokerModel = list.get(0);
            destinationMap.put(destination, brokerModel);
            return brokerModel.getTransport(messageDistribution);
        }
    }

    @Override
    public void addMessageDistribution(MessageDistribution messageDistribution) {
        messageDistributionList.add(messageDistribution);
        if (isStarted()) {
            for (BrokerModel brokerModel : brokerModelMap.values()) {
                try {
                    brokerModel.createTransport(messageDistribution);
                } catch (Exception e) {
                    LOG.warn("Failed to create transport to " + brokerModel + " for " + messageDistribution);
                }
            }
        }
    }

    @Override
    public void removeMessageDistribution(MessageDistribution messageDistribution) {
        messageDistributionList.remove(messageDistribution);
        for (BrokerModel brokerModel : brokerModelMap.values()) {
            brokerModel.removeTransport(messageDistribution);
        }
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        if (poller != null) {
            poller.cancel(true);
        }
        for (BrokerModel model : brokerModelMap.values()) {
            serviceStopper.stop(model);
        }
        serviceStopper.stop(brokerCoordinator);
    }

    @Override
    protected void doStart() throws Exception {
        String brokerName = Systems.getEnvVarOrSystemProperty("BROKER_NAME", getBrokerName());
        setBrokerName(brokerName);
        String groupName = Systems.getEnvVarOrSystemProperty("GROUP_NAME", getGroupName());
        setGroupName(groupName);
        String brokerSelector = Systems.getEnvVarOrSystemProperty("BROKER_SELECTOR", getBrokerSelector());
        setBrokerSelector(brokerSelector);
        setGroupName(groupName);
        Number pollTime = Systems.getEnvVarOrSystemProperty("POLL_INTERVAL", getPollTime());
        setPollTime(pollTime.intValue());
        String brokerTemplateLocation = Systems.getEnvVarOrSystemProperty("BBROKER_TEMPLATE_LOCATION", getBrokerTemplateLocation());
        setBrokerTemplateLocation(brokerTemplateLocation);
        String coordinationType = Systems.getEnvVarOrSystemProperty("BROKER_COORDINATION_TYPE", getBrokerCoordinatorType());
        setBrokerCoordinatorType(coordinationType);
        brokerCoordinator = BrokerCoordinatorFactory.getCoordinator(brokerCoordinatorType);
        brokerCoordinator.start();
        KubernetesFactory kubernetesFactory = new KubernetesFactory();
        kubernetes = new KubernetesClient(kubernetesFactory);
        clients = new JolokiaClients(kubernetes);
        //this will create the broker ReplicationController if it doesn't exist
        this.replicationControllerId = getOrCreateBrokerReplicationControllerId();
        pollBrokers();
        if (brokerModelMap.isEmpty()) {
            createBroker();
        }
        Runnable run = new Runnable() {
            @Override
            public void run() {
                try {
                    scheduleTasks();

                } catch (Throwable e) {
                    LOG.error("Failed to validate MQ load: ", e);
                }
            }
        };
        poller = brokerStateInfo.getController().scheduleAtFixedRate(run, getPollTime(), getPollTime() * 2);
        for (BrokerModel model : brokerModelMap.values()) {
            model.start();
        }

    }

    private void scheduleTasks() {
        pollBrokers();
        checkScaling();
        distributeLoad();
    }

    private void distributeLoad() {
        if (!scalingInProgress.isWorking()) {
            if (brokerModelMap.size() > 1) {
                List<BrokerModel> list = new ArrayList<>(brokerModelMap.values());
                Collections.sort(list);

                final BrokerModel leastLoaded = list.get(0);
                final BrokerModel mostLoaded = list.get(list.size() - 1);
                if (mostLoaded.areBrokerLimitsExceeded() || mostLoaded.areDestinationLimitsExceeded()) {
                    //move queues to least loaded
                    Set<ActiveMQDestination> queues = mostLoaded.getQueues();
                    //take half the queues
                    List<ActiveMQDestination> copyList = new ArrayList<>();
                    if (queues.size() > 1) {
                        List<ActiveMQDestination> sortedList = new ArrayList<>(queues);
                        Collections.sort(sortedList, new Comparator<ActiveMQDestination>() {
                            @Override
                            public int compare(ActiveMQDestination dest1, ActiveMQDestination dest2) {
                                int depth1 = mostLoaded.getDepth(dest1);
                                int depth2 = mostLoaded.getDepth(dest2);
                                int result = depth2 - depth1;
                                if (result == 0) {
                                    result = mostLoaded.getNumberOfProducers(dest2) - mostLoaded.getNumberOfProducers(dest1);
                                    if (result == 0) {
                                        result = mostLoaded.getNumberOfConsumers(dest2) - mostLoaded.getNumberOfConsumers(dest1);
                                    }
                                }
                                return result;
                            }
                        });
                        int toCopy = sortedList.size() / 2;
                        for (int i = 0; i < toCopy; i++) {
                            copyList.add(sortedList.get(i));
                        }
                    } else {
                        copyList.addAll(queues);
                    }
                    copyDestinations(mostLoaded, leastLoaded, copyList);
                }
            }
        }
        scalingInProgress.finished(brokerModelMap.size());
    }

    private void checkScaling() {
        boolean scaledBack = false;
        //can we scale back ?
        if (brokerModelMap.size() > 1) {
            List<BrokerModel> list = new ArrayList<>(brokerModelMap.values());
            Collections.sort(list);
            int load = 0;
            for (BrokerModel model : brokerModelMap.values()) {
                load += model.load();
            }
            if ((load * 100) / list.size() < 50) {
                scaledBack = true;
                final BrokerModel leastLoaded = list.get(0);
                final BrokerModel mostLoaded = list.get(list.size() - 1);
                if (copyDestinations(leastLoaded, mostLoaded)) {
                    destroyBroker(leastLoaded);
                } else {
                    LOG.error("Scale back failed");
                }
            }
        }

        if (!scaledBack) {
            //do we need more brokers ?
            for (BrokerModel model : brokerModelMap.values()) {
                if (model.areBrokerLimitsExceeded() || model.areDestinationLimitsExceeded()) {
                    createBroker();
                    break;
                }
            }
        }
    }

    private void pollBrokers() {
        try {
            Map<String, Pod> podMap = KubernetesHelper.getSelectedPodMap(kubernetes, getBrokerSelector());
            Collection<Pod> pods = podMap.values();
            LOG.info("Checking " + brokerSelector + ": groupSize = " + pods.size());
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

                BrokerModel brokerModel = brokerModelMap.get(brokerId.toString());
                if (brokerModel == null) {
                    BrokerView brokerView = new BrokerView();
                    brokerView.setBrokerName(brokerName.toString());
                    brokerView.setBrokerId(brokerId.toString());
                    brokerView.setUri(uri.toString());
                    brokerModel = new BrokerModel(pod, brokerView);
                    brokerModel.start();
                    brokerModelMap.put(brokerId.toString(), brokerModel);
                    //add transports
                    for (MessageDistribution messageDistribution : messageDistributionList) {
                        brokerView.createTransport(messageDistribution);
                    }
                } else {
                    //transport might not be valid
                    brokerModel.setUri(uri.toString());
                    brokerModel.updateTransport();
                }

                BrokerOverview brokerOverview = new BrokerOverview(brokerId.toString(), brokerName.toString());

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
        populateDestinations(client, root, DestinationOverview.Type.QUEUE, brokerOverview);
        populateDestinations(client, root, DestinationOverview.Type.TOPIC, brokerOverview);
        return brokerOverview;
    }

    private BrokerOverview populateDestinations(J4pClient client, ObjectName root, DestinationOverview.Type type, BrokerOverview brokerOverview) {

        try {
            Hashtable<String, String> props = root.getKeyPropertyList();
            props.put("destinationType", type == DestinationOverview.Type.QUEUE ? "Queue" : "Topic");
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
                    ActiveMQDestination destination = type == DestinationOverview.Type.QUEUE ? new ActiveMQQueue(name) : new ActiveMQTopic(name);
                    DestinationOverview destinationOverview = new DestinationOverview(destination);
                    destinationOverview.setNumberOfConsumers(Integer.parseInt(consumerCount));
                    destinationOverview.setNumberOfProducers(Integer.parseInt(producerCount));
                    destinationOverview.setQueueDepth(Integer.parseInt(queueSize));
                    brokerOverview.addDestinationStatistics(destinationOverview);
                }
            }
        } catch (Exception ex) {
            // Destinations don't exist yet on the broker
            LOG.debug("populateDestinations failed", ex);
        }
        return brokerOverview;
    }

    private void createBroker() {
        int desiredNumber = brokerModelMap.size() + 1;
        if (scalingInProgress.startWork(desiredNumber)) {
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

    private void destroyBroker(BrokerModel brokerModel) {
        int desiredNumber = brokerModelMap.size() - 1;
        if (scalingInProgress.startWork(desiredNumber)) {
            try {
                String id = getOrCreateBrokerReplicationControllerId();
                ReplicationController replicationController = kubernetes.getReplicationController(id);
                int currentDesiredNumber = replicationController.getDesiredState().getReplicas();
                if (desiredNumber == (currentDesiredNumber - 1)) {
                    replicationController.getDesiredState().setReplicas(desiredNumber);
                    brokerModelMap.remove(brokerModel.getBrokerId());
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

    private boolean copyDestinations(BrokerModel from, BrokerModel to) {
        List<ActiveMQDestination> list = new ArrayList<>(from.getQueues());
        return copyDestinations(from, to, list);
    }

    private boolean copyDestinations(BrokerModel from, BrokerModel to, Collection<ActiveMQDestination> destinations) {
        boolean result = false;
        if (!destinations.isEmpty()) {
            try {
                //lock the brokers
                from.lock();
                to.lock();
                //move the queues
                CopyDestinationWorker copyDestinationWorker = new CopyDestinationWorker(brokerStateInfo.getAsyncExectutors(), from.getUri(), to.getUri());
                for (ActiveMQDestination destination : destinations) {
                    copyDestinationWorker.addDestinationToCopy(destination);
                    destinationMap.remove(destination, from);
                }
                copyDestinationWorker.start();
                copyDestinationWorker.aWait();
                //update the sharding map
                for (ActiveMQDestination destination : destinations) {
                    destinationMap.put(destination, to);
                }
                result = true;

            } catch (Exception e) {
                LOG.error("Failed in copy from " + from + " to " + to, e);
            } finally {
                from.unlock();
                to.unlock();
            }
        } else {
            result = true;
        }
        return result;
    }

    private static class WorkInProgress {
        private final AtomicBoolean working = new AtomicBoolean();
        private final AtomicInteger expectedCount = new AtomicInteger();

        boolean startWork(int countWhenFinished) {
            boolean result = working.compareAndSet(false, true);
            if (result) {
                expectedCount.set(countWhenFinished);
            }
            return result;
        }

        boolean isWorking() {
            return working.get();
        }

        boolean finished(int currentCount) {
            boolean result = false;
            if (expectedCount.compareAndSet(expectedCount.get(), currentCount)) {
                working.compareAndSet(true, false);
                result = true;
            }
            return result;
        }
    }
}
