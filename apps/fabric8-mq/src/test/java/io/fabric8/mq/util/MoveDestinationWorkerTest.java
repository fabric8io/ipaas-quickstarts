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

package io.fabric8.mq.util;

import io.fabric8.mq.AsyncExecutors;
import io.fabric8.mq.coordination.brokers.BrokerModel;
import io.fabric8.mq.coordination.brokers.BrokerView;
import io.fabric8.mq.model.Model;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQDestination;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(WeldJUnitRunner.class)
public class MoveDestinationWorkerTest {
    private final String URI1 = "tcp://localhost:61616";
    private final String URI2 = "tcp://localhost:61617";
    BrokerService brokerService1;
    BrokerService brokerService2;
    private List<ActiveMQDestination> destinationList = new ArrayList<>();
    @Inject
    private AsyncExecutors asyncExecutors;
    @Inject
    private Model model;

    @Before
    public void setUp() throws Exception {
        brokerService1 = new BrokerService();
        brokerService1.setBrokerName("broker1");
        brokerService1.setPersistent(false);
        brokerService1.setUseJmx(false);
        brokerService1.addConnector(URI1);
        brokerService1.start();

        brokerService2 = new BrokerService();
        brokerService2.setBrokerName("broker1");
        brokerService2.setPersistent(false);
        brokerService2.setUseJmx(false);
        brokerService2.addConnector(URI2);
        brokerService2.start();
        for (int i = 0; i < 10; i++) {
            destinationList.add(ActiveMQDestination.createDestination("Queue-" + i, ActiveMQDestination.QUEUE_TYPE));
        }
        Connection connection = new ActiveMQConnectionFactory(URI1).createConnection();
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer messageProducer = session.createProducer(null);
        for (ActiveMQDestination destination : destinationList) {
            for (int i = 0; i < 100; i++) {
                TextMessage message = session.createTextMessage("message:" + i);
                messageProducer.send(destination, message);
            }
        }
        connection.close();
        asyncExecutors.start();
        model.start();

    }

    @After
    public void tearDown() throws Exception {
        if (brokerService1 != null) {
            brokerService1.stop();
        }
        if (brokerService2 != null) {
            brokerService2.stop();
            brokerService2.waitUntilStopped();
        }
        if (asyncExecutors != null) {
            asyncExecutors.stop();
        }
        if (model != null) {
            model.stop();
        }
    }

    @Test
    public void doTest() throws Exception {
        BrokerView brokerView1 = new BrokerView();
        brokerView1.setUri(brokerService1.getDefaultSocketURIString());
        BrokerModel brokerModel1 = new BrokerModel(null, brokerView1, model);
        brokerModel1.start();

        BrokerView brokerView2 = new BrokerView();
        brokerView2.setUri(brokerService2.getDefaultSocketURIString());
        BrokerModel brokerModel2 = new BrokerModel(null, brokerView2, model);
        brokerModel2.start();

        MoveDestinationWorker moveDestinationWorker = new MoveDestinationWorker(asyncExecutors, brokerModel1, brokerModel2);
        for (ActiveMQDestination destination : destinationList) {
            moveDestinationWorker.addDestinationToCopy(destination);
        }
        moveDestinationWorker.start();
        moveDestinationWorker.aWait(1, TimeUnit.MINUTES);
        Assert.assertEquals(moveDestinationWorker.getCompletedList(), destinationList);

    }
}
