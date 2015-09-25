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

import io.fabric8.mq.util.TestConnection;
import io.fabric8.mq.util.WeldJUnitRunner;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.transport.Transport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.concurrent.CountDownLatch;

@RunWith(WeldJUnitRunner.class)
public class MQControllerInternalsTest {
    @Inject
    TestController testController;
    Connection senderConnection;
    Connection consumerConnection;

    @Before
    public void setUp() throws Exception {

        //testController = new TestController();
        testController.start();
        Transport transport = testController.connect();

        senderConnection = new TestConnection(transport);
        senderConnection.start();

        transport = testController.connect();

        consumerConnection = new TestConnection(transport);
        consumerConnection.start();

    }

    @After
    public void tearDown() throws Exception {
        if (senderConnection != null) {
            senderConnection.close();
        }
        if (consumerConnection != null) {
            consumerConnection.close();
        }
        if (testController != null) {
            testController.stop();
        }
    }

    @Test
    public void testInternals() throws Exception {
        final int number = 5000;
        CountDownLatch countDownLatch = new CountDownLatch(number * 2);

        Queue queue1 = new ActiveMQQueue("test.queue.1");
        Queue queue2 = new ActiveMQQueue("test.queue.2");

        Session session = consumerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageConsumer consumer1 = session.createConsumer(queue1);
        session = consumerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageConsumer consumer2 = session.createConsumer(queue2);

        session = senderConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer1 = session.createProducer(queue1);

        session = senderConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer2 = session.createProducer(queue2);

        Thread t1 = new Thread(new TestRun(countDownLatch, consumer1, number));
        t1.start();

        Thread t2 = new Thread(new TestRun(countDownLatch, consumer2, number));
        t2.start();

        for (int i = 0; i < number; i++) {
            Message message = session.createTextMessage("test:" + i);
            producer1.send(message);
            producer2.send(message);
        }

        countDownLatch.await();
    }

    private class TestRun implements Runnable {
        private final CountDownLatch countDownLatch;
        private final MessageConsumer messageConsumer;
        private final int count;

        TestRun(CountDownLatch countDownLatch, MessageConsumer messageConsumer, int count) {
            this.countDownLatch = countDownLatch;
            this.messageConsumer = messageConsumer;
            this.count = count;
        }

        public void run() {
            try {
                for (int i = 0; i < count; i++) {
                    TextMessage message = (TextMessage) messageConsumer.receive();
                    countDownLatch.countDown();
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}
