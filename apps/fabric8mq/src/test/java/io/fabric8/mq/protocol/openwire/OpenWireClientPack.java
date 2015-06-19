/*
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

package io.fabric8.mq.protocol.openwire;

import io.fabric8.mq.protocol.BaseClientPack;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.util.ServiceStopper;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class OpenWireClientPack extends BaseClientPack {

    private List<Connection> connectionList = new ArrayList<>();
    private ConnectionFactory connectionFactory;
    private Connection singleConnection;
    private Map<Destination, Connection> connectionMap = new HashMap<>();

    @Override
    protected void doStart() throws Exception {
        String uri = "tcp://" + getHost() + ":" + getPort();
        connectionFactory = new ActiveMQConnectionFactory(uri);

        if (isOneConnection()) {
            singleConnection = connectionFactory.createConnection();
            singleConnection.start();
            connectionList.add(singleConnection);
        }
        super.doStart();
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        for (Connection connection : connectionList) {
            connection.close();
        }
        connectionMap.clear();
        super.doStop(serviceStopper);
    }

    @Override
    protected Runnable createConsumers(String destinationName, final CountDownLatch countDownLatch) throws Exception {
        final ActiveMQDestination destination = ActiveMQDestination.createDestination(destinationName, ActiveMQDestination.QUEUE_TYPE);
        return new Runnable() {
            @Override
            public void run() {
                try {
                    Connection connection = getConnection(destination);
                    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    MessageConsumer consumer = session.createConsumer(destination);
                    consumer.setMessageListener(new MessageListener() {
                        @Override
                        public void onMessage(Message message) {
                            countDownLatch.countDown();
                        }
                    });
                    if (!countDownLatch.await(TIMEOUT, TimeUnit.MINUTES)) {
                        throw new IllegalStateException("Timed out waiting for message consumption to finish");
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        };
    }

    @Override
    protected Runnable createProducers(String destinationName, final CountDownLatch countDownLatch, final int numberOfMessages) throws Exception {
        final ActiveMQDestination destination = ActiveMQDestination.createDestination(destinationName, ActiveMQDestination.QUEUE_TYPE);
        return new Runnable() {
            @Override
            public void run() {
                try {
                    Connection connection = getConnection(destination);
                    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    MessageProducer producer = session.createProducer(destination);
                    for (int i = 0; i < numberOfMessages; i++) {
                        TextMessage message = session.createTextMessage("test:" + i);
                        producer.send(message);
                        countDownLatch.countDown();
                    }

                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        };
    }

    @Override
    protected void destroyConsumers(String destinationName) throws Exception {

    }

    @Override
    protected void destroyProducers(String destinationName) throws Exception {

    }

    protected Connection getConnection(Destination destination) throws Exception {
        if (singleConnection != null) {
            return singleConnection;
        } else if (isConnectionPerDestination()) {
            Connection connection = connectionMap.get(destination);
            if (connection == null) {
                connection = connectionFactory.createConnection();
                connection.start();
                connectionMap.put(destination, connection);
                connectionList.add(connection);
            }
            return connection;
        } else {
            ActiveMQConnection connection = (ActiveMQConnection) connectionFactory.createConnection();
            connection.start();
            connectionList.add(connection);
            return connection;
        }
    }
}
