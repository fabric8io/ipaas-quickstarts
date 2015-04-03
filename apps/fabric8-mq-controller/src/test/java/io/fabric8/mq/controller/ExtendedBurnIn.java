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
package io.fabric8.mq.controller;

import io.fabric8.gateway.api.apimanager.ApiManager;
import io.fabric8.gateway.api.handlers.http.IMappedServices;
import io.fabric8.gateway.handlers.http.HttpGatewayServer;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.curator.framework.CuratorFramework;
import org.junit.After;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 */
public class ExtendedBurnIn {

    private static final transient Logger LOG = LoggerFactory.getLogger(ExtendedBurnIn.class);

    final protected ArrayList<MQController> gateways = new ArrayList<>();
    final HashMap<String, IMappedServices> mappedServices = new HashMap<String, IMappedServices>();
    final ApiManager apiManager = new ApiManager();
    HttpGatewayServer httpGatewayServer;

    protected void println(Object msg) {
        LOG.info(msg.toString());
    }

    public MQController startDetectingGateway() throws Exception {

        MQController gateway = new MQController();
        gateway.setPort(0);
        //SslConfig sslConfig = new SslConfig(new File(basedir(), "src/test/resources/server.ks"), "password");
        //sslConfig.setKeyPassword("password");
        //gateway.setSslConfig(sslConfig);
        //gateway.setDefaultVirtualHost("broker1");
        //gateway.setConnectionTimeout(5000);
        gateway.setNumberOfServers(1);
        gateway.start();

        gateways.add(gateway);
        return gateway;
    }

    @After
    public void stopGateways() {
        for (MQController gateway : gateways) {
            gateway.close();
        }
        gateways.clear();
    }

    protected File basedir() {
        try {
            File file = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getFile());
            file = file.getParentFile().getParentFile().getCanonicalFile();
            if (file.isDirectory()) {
                return file.getCanonicalFile();
            } else {
                return new File(".").getCanonicalFile();
            }
        } catch (Throwable e) {
            return new File(".");
        }
    }

    public void lotsOfClientLoad() throws Exception {

        CuratorFramework client;

        MQController gateway = new MQController();
        gateway.setPort(0);
        gateway.setNumberOfServers(1);
        gateway.setNumberOfMultiplexers(2);
        gateway.start();

        // Run some concurrent load against the broker via the gateway...
        final ConnectionFactory factory = new ActiveMQConnectionFactory("failover:(tcp://localhost:" + gateway.getBoundPort() + "?wireFormat.maxInactivityDuration=2000)");

        for (int j = 0; j < 2; j++) {
            final int finalJ = j;
            new Thread("JMS Client: " + finalJ) {
                final AtomicInteger client = new AtomicInteger(finalJ);

                @Override
                public void run() {
                    try {

                        Connection connection = factory.createConnection();
                        System.err.println("CLIENT(" + client + ") CREATING CONNECTION(" + finalJ + ") ....");
                        connection.start();
                        for (int i = 0; i < 10; i++) {
                            System.err.println("CLIENT(" + client + ") CREATING SESSION(" + i + ") ....");
                            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                            System.err.println("CREATED SESSION(" + i + ")");
                            MessageConsumer consumer = session.createConsumer(session.createTopic("FOO"));
                            MessageProducer producer = session.createProducer(session.createTopic("FOO"));
                            System.err.println("CREATED PRODIUCER(" + i + ")");
                            producer.send(session.createTextMessage("Hello:" + i));
                            System.err.println("SENT MESSAGE ");
                            TextMessage message = (TextMessage) consumer.receive(5000);
                        }

                        connection.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }.start();
        }

        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        // Lets monitor memory usage for 10 min..
        for (int i = 0; i < 60 * 10; i++) {
            Runtime.getRuntime().gc();
            Thread.sleep(10000);
            long usedMB = ((Long) ((CompositeData) mBeanServer.getAttribute(new ObjectName("java.lang:type=Memory"), "HeapMemoryUsage")).get("used")).longValue() / (1024 * 1024);
            LOG.info("Using {} MB of heap.", usedMB);
        }

    }

}
