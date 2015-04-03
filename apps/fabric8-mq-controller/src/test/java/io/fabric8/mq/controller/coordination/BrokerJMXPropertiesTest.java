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

import org.apache.activemq.broker.BrokerService;
import org.jolokia.client.J4pClient;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pResponse;
import org.jolokia.jvmagent.JolokiaServer;
import org.jolokia.jvmagent.JvmAgentConfig;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.Assert;

import javax.management.ObjectName;

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

    @Test
    public void testGetBrokerName() throws Exception {
        String type = "org.apache.activemq:type=Broker,*";
        String attribute = "BrokerName";
        ObjectName objectName = new ObjectName(type);
        J4pResponse<J4pReadRequest> result = client.execute(new J4pReadRequest(objectName, attribute));
        JSONObject jsonObject = result.getValue();
        Assert.notNull(jsonObject);
        String name = jsonObject.keySet().iterator().next().toString();
        Assert.notNull(name);
        System.err.println("BROKER NAME = " + name);
    }
}
