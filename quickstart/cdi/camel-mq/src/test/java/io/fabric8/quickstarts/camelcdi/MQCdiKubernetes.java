/*
 * Copyright 2005-2015 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.quickstarts.camelcdi;

import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import io.fabric8.annotations.ServiceName;
import io.fabric8.cdi.deltaspike.DeltaspikeTestBase;
import io.fabric8.quickstarts.camelcdi.mq.ActiveMQConfigurer;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;

@RunWith(Arquillian.class)
public class MQCdiKubernetes {

    @Inject
    @ServiceName("fabric8mq")
    ActiveMQConnectionFactory connectionFactory;

    @Deployment
    public static WebArchive createDeployment() {
        return DeltaspikeTestBase.createDeployment()
                .addClasses(DeltaspikeTestBase.getDeltaSpikeHolders())
                .addClasses(ActiveMQConfigurer.class);
    }

    @Test
    public void testConnectionFactory() throws Exception {
        Assert.assertNotNull(connectionFactory);
        Connection connection = connectionFactory.createConnection();
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        ActiveMQQueue queue = new ActiveMQQueue("test.queue");
        MessageProducer producer = session.createProducer(queue);
        MessageConsumer consumer = session.createConsumer(queue);

        ActiveMQTextMessage msg = new ActiveMQTextMessage();
        String text = "hello";
        msg.setText(text);
        producer.send(msg);

        Message result = consumer.receive();
        Assert.assertThat(result, instanceOf(TextMessage.class));
        Assert.assertThat(((TextMessage) result).getText(), equalTo(text));
    }
}
