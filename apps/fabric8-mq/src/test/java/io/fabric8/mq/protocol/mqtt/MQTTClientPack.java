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

import io.fabric8.mq.protocol.BaseClientPack;
import org.apache.activemq.util.ServiceStopper;
import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.fusesource.hawtbuf.UTF8Buffer.utf8;

public class MQTTClientPack extends BaseClientPack {

    private List<BlockingConnection> connectionList = new ArrayList<>();
    private MQTT mqtt;
    private BlockingConnection singleConnection;
    private Map<String, BlockingConnection> connectionMap = new HashMap<>();

    @Override
    protected void doStart() throws Exception {
        String uri = "tcp://" + getHost() + ":" + getPort();

        mqtt = new MQTT();
        mqtt.setConnectAttemptsMax(1);
        mqtt.setReconnectAttemptsMax(0);
        mqtt.setHost(getHost(), getPort());

        if (isOneConnection()) {
            singleConnection = mqtt.blockingConnection();
            singleConnection.connect();
            connectionList.add(singleConnection);
        }
        super.doStart();
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        for (BlockingConnection connection : connectionList) {
            connection.disconnect();
        }
        connectionMap.clear();
        super.doStop(serviceStopper);
    }

    @Override
    protected Runnable createConsumers(final String destinationName, final CountDownLatch countDownLatch) throws Exception {
        final Topic[] topics = {new Topic(utf8(destinationName), QoS.values()[1])};

        return new Runnable() {
            @Override
            public void run() {
                try {
                    BlockingConnection connection = getConnection(destinationName);
                    connection.subscribe(topics);
                    while (countDownLatch.getCount() != 0) {
                        org.fusesource.mqtt.client.Message message = connection.receive();
                        if (message != null) {
                            countDownLatch.countDown();
                        }
                    }
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
    protected Runnable createProducers(final String destinationName, final CountDownLatch countDownLatch, final int numberOfMessages) throws Exception {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    BlockingConnection connection = getConnection(destinationName);

                    for (int i = 0; i < numberOfMessages; i++) {
                        String payload = "test:" + i;
                        connection.publish(destinationName, payload.getBytes(), QoS.values()[1], false);
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

    protected BlockingConnection getConnection(String destination) throws Exception {
        if (singleConnection != null) {
            return singleConnection;
        } else if (isConnectionPerDestination()) {
            BlockingConnection connection = connectionMap.get(destination);
            if (connection == null) {
                connection = mqtt.blockingConnection();
                connection.connect();
                connectionMap.put(destination, connection);
                connectionList.add(connection);
            }
            return connection;
        } else {
            BlockingConnection connection = mqtt.blockingConnection();
            connection.connect();
            connectionList.add(connection);
            return connection;
        }
    }
}
