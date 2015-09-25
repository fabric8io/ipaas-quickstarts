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

package io.fabric8.mq.sharding;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.proxy.ProxyConnector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.jms.Connection;
import java.net.URI;

public class JMSSendReceiveProxyTest {

    private final static String LOCAL = "tcp://localhost:61616";
    private final static String REMOTE = "tcp://localhost:61617";
    private BrokerService remote;
    private BrokerService local;
    private ActiveMQConnectionFactory connectionFactory;

    @Before
    public void setUp() throws Exception {
        remote = new BrokerService();
        remote.setUseJmx(false);
        remote.setPersistent(false);
        remote.addConnector(REMOTE);
        remote.start();
        remote.waitUntilStarted();
        local = new BrokerService();
        local.setUseJmx(false);
        local.setPersistent(false);
        ProxyConnector proxyConnector = new ProxyConnector();
        proxyConnector.setBind(new URI(LOCAL));
        proxyConnector.setRemote(new URI(REMOTE));
        local.addProxyConnector(proxyConnector);
        local.start();
        local.waitUntilStarted();
        connectionFactory = new ActiveMQConnectionFactory(LOCAL);
    }

    @After
    public void tearDown() throws Exception {
        if (local != null) {
            local.stop();
            local.waitUntilStopped();
        }
        if (remote != null) {
            remote.stop();
            remote.waitUntilStopped();
        }
    }

    @Test
    public void simpleTest() throws Exception {
        Connection connection = connectionFactory.createConnection();
        connection.start();
    }
}
