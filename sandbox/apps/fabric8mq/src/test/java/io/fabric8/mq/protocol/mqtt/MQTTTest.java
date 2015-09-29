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
package io.fabric8.mq.protocol.mqtt;

import io.fabric8.mq.protocol.TestProtocolServer;
import io.fabric8.mq.util.WeldJUnitRunner;
import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static org.fusesource.hawtbuf.UTF8Buffer.utf8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(WeldJUnitRunner.class)
public class MQTTTest {
    @Inject
    private TestProtocolServer testProtocolServer;

    @Before
    public void setUp() throws Exception {
        testProtocolServer.setProtocolTransportFactory(new MQTTTransportFactory());
        testProtocolServer.start();
    }

    @After
    public void tearDown() throws Exception {
        if (testProtocolServer != null) {
            testProtocolServer.stop();
        }
    }

    @Test
    public void testSendAndReceiveAtLeastOnce() throws Exception {
        MQTT consumerClient = createMQTTTcpConnection("consumer", true);
        consumerClient.setConnectAttemptsMax(0);
        consumerClient.setReconnectAttemptsMax(0);
        BlockingConnection consumeConnection = consumerClient.blockingConnection();
        consumeConnection.connect();

        String topic = "foo";

        Topic[] topics = {new Topic(utf8(topic), QoS.values()[1])};
        consumeConnection.subscribe(topics);

        MQTT producerClient = createMQTTTcpConnection("producer", true);

        producerClient.setConnectAttemptsMax(0);
        producerClient.setReconnectAttemptsMax(0);
        BlockingConnection producerConnection = producerClient.blockingConnection();
        producerConnection.connect();

        for (int i = 0; i < 100; i++) {
            String payload = "Test Message: " + i;
            producerConnection.publish(topic, payload.getBytes(), QoS.values()[1], false);
            Message message = consumeConnection.receive(10, TimeUnit.SECONDS);
            assertNotNull("Should get a message", message);
            assertEquals(payload, new String(message.getPayload()));
        }
        consumeConnection.disconnect();
        producerConnection.disconnect();
    }

    private MQTT createMQTTTcpConnection(String clientId, boolean clean) throws Exception {
        MQTT mqtt = new MQTT();
        mqtt.setConnectAttemptsMax(1);
        mqtt.setReconnectAttemptsMax(0);
        if (clientId != null) {
            mqtt.setClientId(clientId);
        }
        mqtt.setCleanSession(clean);
        mqtt.setHost("localhost", testProtocolServer.getBoundPort());
        return mqtt;
    }

}
