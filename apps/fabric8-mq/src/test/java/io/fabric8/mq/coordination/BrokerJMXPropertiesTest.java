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

import io.fabric8.mq.util.BrokerJmxUtils;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.jolokia.client.J4pClient;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pResponse;
import org.jolokia.jvmagent.JolokiaServer;
import org.jolokia.jvmagent.JvmAgentConfig;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.List;

public class BrokerJMXPropertiesTest {
    static final String BROKER_NAME = "fred";
    static final int JOLOKIA_PORT = 8189;
    private BrokerService brokerService;
    private JolokiaServer jolokiaServer;
    private J4pClient client;

    @Before
    public void setUp() throws Exception {
        brokerService = new BrokerService();
        brokerService.setBrokerName(BROKER_NAME);
        brokerService.setUseJmx(true);
        brokerService.setPersistent(false);
        brokerService.addConnector("tcp://localhost:0");
        brokerService.start();
        brokerService.waitUntilStarted();
        JvmAgentConfig config = new JvmAgentConfig("host=localhost,port=" + JOLOKIA_PORT);
        jolokiaServer = new JolokiaServer(config, false);
        jolokiaServer.start();
        String url = "http://localhost:" + JOLOKIA_PORT + "/jolokia";
        client = new J4pClient(url);

    }

    @After
    public void tearDown() throws Exception {
        if (jolokiaServer != null) {
            jolokiaServer.stop();
        }
        if (brokerService != null) {
            brokerService.stop();
            brokerService.waitUntilStopped();
        }
    }

    public String getBrokerName() throws Exception {
        String type = "org.apache.activemq:type=Broker,*";
        String attribute = "BrokerName";
        ObjectName objectName = new ObjectName(type);
        J4pResponse<J4pReadRequest> result = client.execute(new J4pReadRequest(objectName, attribute));
        JSONObject jsonObject = result.getValue();
        Assert.assertNotNull(jsonObject);
        Object key = jsonObject.keySet().iterator().next();
        JSONObject value = (JSONObject) jsonObject.get(key);
        String name = value.values().iterator().next().toString();
        System.err.println("BROKER NAME = " + name);
        Assert.assertNotNull(value);

        Assert.assertEquals(BROKER_NAME, name);
        return name;
    }

    @Test
    public void testNumberOfTopicProducers() throws Exception {
        String uriString = brokerService.getDefaultSocketURIString();
        ConnectionFactory factory = new ActiveMQConnectionFactory(uriString);
        Connection connection = factory.createConnection();
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        int numberOfProducers = 10;
        List<MessageProducer> producers = new ArrayList<>();
        Topic topic = session.createTopic("topic.test");
        for (int i = 0; i < numberOfProducers; i++) {

            MessageProducer producer = session.createProducer(topic);
            producers.add(producer);

        }
        ObjectName root = BrokerJmxUtils.getRoot(client);
        Assert.assertNotNull(root);
        List<ObjectName> list = BrokerJmxUtils.getDestinations(client, root, "Topic");
        Assert.assertNotNull(list);
        Assert.assertFalse(list.isEmpty());
        ObjectName result = null;
        for (ObjectName objectName : list) {
            if (objectName.getKeyProperty("destinationName").equals("topic.test")) {
                result = objectName;
            }
        }
        Assert.assertNotNull(result);
        Object producerCount = BrokerJmxUtils.getAttribute(client, result, "ProducerCount");
        Assert.assertNotNull(producerCount);
        int pc = Integer.parseInt(producerCount.toString());
        Assert.assertEquals(pc, numberOfProducers);
        connection.close();
    }

}
