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

package io.fabric8.mq;

import io.fabric8.mq.model.BrokerControl;
import io.fabric8.mq.util.WeldJUnitRunner;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/*
    Test the entire thing - but will use the BrokerControlTestImpl instead of KubernetesControl
    */
@RunWith(WeldJUnitRunner.class)
public class CamelTest {
    private static final Logger LOG = LoggerFactory.getLogger(CamelTest.class);
    @Inject
    BrokerControl brokerControl;
    @Inject
    private Fabric8MQ fabric8MQ;

    @Before
    public void doStart() throws Exception {
        fabric8MQ.getFabric8MQStatus().setControllerPort(0);
        fabric8MQ.setBrokerControl(brokerControl);
        String camelRoute = "<from uri=\"mq:topic:>\"/> <to uri=\"mq:queue:>\"/>";

        System.err.println("CAMEL ROUTE = " + camelRoute);
        fabric8MQ.getFabric8MQStatus().setCamelRoutes(camelRoute);
        fabric8MQ.getFabric8MQStatus().setNumberOfMultiplexers(1);
        fabric8MQ.getFabric8MQStatus().setNumberOfSevers(1);
        fabric8MQ.start();
    }

    @After
    public void doStop() throws Exception {
        if (fabric8MQ != null) {
            fabric8MQ.stop();
        }
    }

    @Test
    public void test() throws Exception {


        final int numberOfMessages = 0;
        final CountDownLatch countDownLatch = new CountDownLatch(numberOfMessages);
        String destinationName = "testtopic";
        String uri = "tcp://0.0.0.0:";
        uri += fabric8MQ.getBoundPort();

        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(uri);
        final Connection connection = connectionFactory.createConnection();
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = new ActiveMQQueue(destinationName);
        MessageConsumer consumer = session.createConsumer(destination);
        consumer.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                countDownLatch.countDown();
            }
        });



        MQTT mqtt = new MQTT();
        mqtt.setHost("0.0.0.0",fabric8MQ.getBoundPort());
        BlockingConnection mqttConnection = mqtt.blockingConnection();
        mqttConnection.connect();

        for (int i = 0; i < numberOfMessages; i++){
            String payload = "test:" + i;
            mqttConnection.publish(destinationName, payload.getBytes(), QoS.values()[1], false);
        }


        mqttConnection.disconnect();
        connection.close();

        countDownLatch.await(numberOfMessages * 100, TimeUnit.SECONDS);
        Assert.assertTrue(countDownLatch.getCount() == 0);

    }
}
