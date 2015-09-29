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

import io.fabric8.mq.coordination.brokers.BrokerModel;
import io.fabric8.mq.model.BrokerModelChangedListener;
import io.fabric8.mq.util.TestConnection;
import io.fabric8.mq.util.WeldJUnitRunner;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.ActiveMQQueue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(WeldJUnitRunner.class)
public class MQInternalScaleUpTest implements BrokerModelChangedListener {
    private final int TIME_OUT_SECONDS = 30;
    private int numberOfDestinations = 5;
    private int numberOfMessages = 10;
    private String destinationName = "test.scaling";
    @Inject
    private TestController testController;
    private CountDownLatch countUpLatch = new CountDownLatch(1);
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    private List<ActiveMQDestination> destinations = new ArrayList<>();

    @BeforeClass
    public static void setUp() {
        System.setProperty("MAX_DESTINATIONS_PER_BROKER", "2");
    }

    @Before
    public void init() throws Exception {
        testController.addBrokerModelChangedListener(this);
        testController.start();

        for (int i = 0; i < numberOfDestinations; i++) {
            String destinationName = this.destinationName + "." + i;
            destinations.add(new ActiveMQQueue(destinationName));
        }
    }

    @After
    public void shutDown() throws Exception {
        testController.stop();
    }

    @Test
    public void scalingTest() throws Exception {
        TestConnection testConnection = testController.createTestConnection("foo");
        for (ActiveMQDestination destination : destinations) {
            Session session = testConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(destination);
            for (int i = 0; i < numberOfMessages; i++) {
                TextMessage textMessage = session.createTextMessage("test:" + i);
                producer.send(textMessage);
            }
            session.close();
        }
        testConnection.close();

        if (!countUpLatch.await(TIME_OUT_SECONDS, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out");
        }
        //we now have 3 Brokers - wait a bit to ensure steady state
        Thread.sleep(5000);
        //we should have 3 brokers
        Collection<BrokerModel> brokerModels = testController.getBrokerModels();
        Assert.assertTrue(3 == brokerModels.size());
        //check all brokers have destinations
        for (BrokerModel brokerModel : brokerModels) {
            Assert.assertFalse(testController.getModel().areDestinationLimitsExceeded(brokerModel));
            Assert.assertTrue(brokerModel.getActiveDestinationCount() > 0);
        }

        //drain all the destinations
        final CountDownLatch messageCounterLatch = new CountDownLatch(destinations.size() * numberOfMessages);
        testConnection = testController.createTestConnection("foo");
        for (ActiveMQDestination destination : destinations) {
            Session session = testConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer consumer = session.createConsumer(destination);
            consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    messageCounterLatch.countDown();
                }
            });
        }
        if (!messageCounterLatch.await(TIME_OUT_SECONDS, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out");
        }
        testConnection.close();
        if (!countDownLatch.await(TIME_OUT_SECONDS, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out");
        }
        Assert.assertEquals(1, testController.getBrokerModels().size());

    }

    @Override
    public void brokerNumberChanged(int numberOfBrokers) {
        System.err.println("ScaleUpTest " + numberOfBrokers);
        if (numberOfBrokers == 3) {
            countUpLatch.countDown();
        }
        if (numberOfBrokers == 1 && countUpLatch.getCount() == 0) {
            countDownLatch.countDown();
        }

    }
}
