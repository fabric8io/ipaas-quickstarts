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
package io.fabric8.mq.protocol.stomp;

import com.thoughtworks.xstream.XStream;
import io.fabric8.mq.protocol.TestProtocolServer;
import io.fabric8.mq.util.WeldJUnitRunner;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.activemq.transport.stomp.Stomp;
import org.apache.activemq.transport.stomp.StompConnection;
import org.apache.activemq.transport.stomp.StompFrame;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.io.IOException;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

@RunWith(WeldJUnitRunner.class)
public class StompTest {
    private final String xmlObject = "<pojo>\n"
                                         + "  <name>Dejan</name>\n"
                                         + "  <city>Belgrade</city>\n"
                                         + "</pojo>";
    private final String jsonObject = "{\"pojo\":{"
                                          + "\"name\":\"Dejan\","
                                          + "\"city\":\"Belgrade\""
                                          + "}}";
    @Rule
    public TestName name = new TestName();
    protected Connection connection;
    protected Session session;
    protected ActiveMQQueue queue;
    protected XStream xstream;
    protected StompConnection stompConnection;
    @Inject
    private TestProtocolServer testProtocolServer;
    private String xmlMap = "<map>\n"
                                + "  <entry>\n"
                                + "    <string>name</string>\n"
                                + "    <string>Dejan</string>\n"
                                + "  </entry>\n"
                                + "  <entry>\n"
                                + "    <string>city</string>\n"
                                + "    <string>Belgrade</string>\n"
                                + "  </entry>\n"
                                + "</map>\n";
    private String jsonMap = "{\"map\":{"
                                 + "\"entry\":["
                                 + "{\"string\":[\"name\",\"Dejan\"]},"
                                 + "{\"string\":[\"city\",\"Belgrade\"]}"
                                 + "]"
                                 + "}}";

    @Before
    public void setUp() throws Exception {
        testProtocolServer.setProtocolTransportFactory(new StompTransportFactory());
        testProtocolServer.start();
        stompConnection = new StompConnection();
        stompConnection.open(createSocket());
        queue = new ActiveMQQueue(getQueueName());
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(testProtocolServer.getBrokerURI());
        connection = connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    @After
    public void tearDown() throws Exception {
        if (stompConnection != null) {
            stompConnection.close();
        }
        if (connection != null) {
            connection.close();
        }
        if (testProtocolServer != null) {
            testProtocolServer.stop();
        }
    }

    @Test
    public void testConnect() throws Exception {

        String connectFrame = "CONNECT\n" + "login:system\n" + "passcode:manager\n" + "request-id:1\n" + "\n" + Stomp.NULL;
        stompConnection.sendFrame(connectFrame);

        String f = stompConnection.receiveFrame();
        assertTrue(f.startsWith("CONNECTED"));
        assertTrue(f.indexOf("response-id:1") >= 0);
    }

    @Test
    public void testSendMessage() throws Exception {

        MessageConsumer consumer = session.createConsumer(queue);

        String frame = "CONNECT\n" + "login:system\n" + "passcode:manager\n\n" + Stomp.NULL;
        stompConnection.sendFrame(frame);

        frame = stompConnection.receiveFrame();
        assertTrue(frame.startsWith("CONNECTED"));

        frame = "SEND\n" + "destination:/queue/" + getQueueName() + "\n\n" + "Hello World" + Stomp.NULL;

        stompConnection.sendFrame(frame);

        TextMessage message = (TextMessage) consumer.receive(2500);
        assertNotNull(message);
        assertEquals("Hello World", message.getText());

        // Make sure that the timestamp is valid - should
        // be very close to the current time.
        long tnow = System.currentTimeMillis();
        long tmsg = message.getJMSTimestamp();
        assertTrue(Math.abs(tnow - tmsg) < 1000);
    }

    @Test
    public void testJMSXGroupIdCanBeSet() throws Exception {

        MessageConsumer consumer = session.createConsumer(queue);

        String frame = "CONNECT\n" + "login:system\n" + "passcode:manager\n\n" + Stomp.NULL;
        stompConnection.sendFrame(frame);

        frame = stompConnection.receiveFrame();
        assertTrue(frame.startsWith("CONNECTED"));

        frame = "SEND\n" + "destination:/queue/" + getQueueName() + "\n" + "JMSXGroupID:TEST\n\n" + "Hello World" + Stomp.NULL;

        stompConnection.sendFrame(frame);

        TextMessage message = (TextMessage) consumer.receive(2500);
        assertNotNull(message);
        assertEquals("TEST", ((ActiveMQTextMessage) message).getGroupID());
    }

    @Test
    public void testSendMessageWithCustomHeadersAndSelector() throws Exception {

        MessageConsumer consumer = session.createConsumer(queue, "foo = 'abc'");

        String frame = "CONNECT\n" + "login:system\n" + "passcode:manager\n\n" + Stomp.NULL;
        stompConnection.sendFrame(frame);

        frame = stompConnection.receiveFrame();
        assertTrue(frame.startsWith("CONNECTED"));

        frame = "SEND\n" + "foo:abc\n" + "bar:123\n" + "destination:/queue/" + getQueueName() + "\n\n" + "Hello World" + Stomp.NULL;

        stompConnection.sendFrame(frame);

        TextMessage message = (TextMessage) consumer.receive(2500);
        assertNotNull(message);
        assertEquals("Hello World", message.getText());
        assertEquals("foo", "abc", message.getStringProperty("foo"));
        assertEquals("bar", "123", message.getStringProperty("bar"));
    }

    @Test
    public void testSendMessageWithStandardHeaders() throws Exception {

        MessageConsumer consumer = session.createConsumer(queue);

        String frame = "CONNECT\n" + "login:system\n" + "passcode:manager\n\n" + Stomp.NULL;
        stompConnection.sendFrame(frame);

        frame = stompConnection.receiveFrame();
        assertTrue(frame.startsWith("CONNECTED"));

        frame = "SEND\n" + "correlation-id:c123\n" + "priority:3\n" + "type:t345\n" + "JMSXGroupID:abc\n" + "foo:abc\n" + "bar:123\n" + "destination:/queue/" + getQueueName() + "\n\n" + "Hello World"
                    + Stomp.NULL;

        stompConnection.sendFrame(frame);

        TextMessage message = (TextMessage) consumer.receive(2500);
        assertNotNull(message);
        assertEquals("Hello World", message.getText());
        assertEquals("JMSCorrelationID", "c123", message.getJMSCorrelationID());
        assertEquals("getJMSType", "t345", message.getJMSType());
        assertEquals("getJMSPriority", 3, message.getJMSPriority());
        assertEquals("foo", "abc", message.getStringProperty("foo"));
        assertEquals("bar", "123", message.getStringProperty("bar"));

        assertEquals("JMSXGroupID", "abc", message.getStringProperty("JMSXGroupID"));
        ActiveMQTextMessage amqMessage = (ActiveMQTextMessage) message;
        assertEquals("GroupID", "abc", amqMessage.getGroupID());
    }

    @Test
    public void testSendMessageWithNoPriorityReceivesDefault() throws Exception {

        MessageConsumer consumer = session.createConsumer(queue);

        String frame = "CONNECT\n" + "login:system\n" + "passcode:manager\n\n" + Stomp.NULL;
        stompConnection.sendFrame(frame);

        frame = stompConnection.receiveFrame();
        assertTrue(frame.startsWith("CONNECTED"));

        frame = "SEND\n" + "correlation-id:c123\n" + "destination:/queue/" + getQueueName() + "\n\n" + "Hello World"
                    + Stomp.NULL;

        stompConnection.sendFrame(frame);

        TextMessage message = (TextMessage) consumer.receive(2500);
        assertNotNull(message);
        assertEquals("Hello World", message.getText());
        assertEquals("getJMSPriority", 4, message.getJMSPriority());
    }

    @Test
    public void testReceipts() throws Exception {

        StompConnection receiver = new StompConnection();
        receiver.open(createSocket());
        String frame = "CONNECT\n" + "login:system\n" + "passcode:manager\n\n" + Stomp.NULL;
        receiver.sendFrame(frame);

        frame = receiver.receiveFrame();
        assertTrue(frame.startsWith("CONNECTED"));

        frame = "SUBSCRIBE\n" + "destination:/queue/" + getQueueName() + "\n" + "ack:auto\n\n" + Stomp.NULL;
        receiver.sendFrame(frame);

        frame = "CONNECT\n" + "login:system\n" + "passcode:manager\n\n" + Stomp.NULL;
        stompConnection.sendFrame(frame);

        frame = stompConnection.receiveFrame();
        assertTrue(frame.startsWith("CONNECTED"));

        frame = "SEND\n" + "destination:/queue/" + getQueueName() + "\n" + "receipt: msg-1\n" + "\n\n" + "Hello World" + Stomp.NULL;
        stompConnection.sendFrame(frame);

        frame = receiver.receiveFrame();
        assertTrue(frame.startsWith("MESSAGE"));
        assertTrue("Stomp Message does not contain receipt request", frame.indexOf(Stomp.Headers.RECEIPT_REQUESTED) == -1);

        frame = stompConnection.receiveFrame();
        assertTrue(frame.startsWith("RECEIPT"));
        assertTrue("Receipt contains correct receipt-id", frame.indexOf(Stomp.Headers.Response.RECEIPT_ID) >= 0);

        frame = "DISCONNECT\n" + "receipt: dis-1\n" + "\n\n" + Stomp.NULL;
        receiver.sendFrame(frame);
        frame = receiver.receiveFrame();
        assertTrue(frame.startsWith("RECEIPT"));
        assertTrue("Receipt contains correct receipt-id", frame.indexOf(Stomp.Headers.Response.RECEIPT_ID) >= 0);
        receiver.close();

        MessageConsumer consumer = session.createConsumer(queue);

        frame = "SEND\n" + "destination:/queue/" + getQueueName() + "\n" + "receipt: msg-1\n" + "\n\n" + "Hello World" + Stomp.NULL;
        stompConnection.sendFrame(frame);

        frame = stompConnection.receiveFrame();
        assertTrue(frame.startsWith("RECEIPT"));
        assertTrue("Receipt contains correct receipt-id", frame.indexOf(Stomp.Headers.Response.RECEIPT_ID) >= 0);

        TextMessage message = (TextMessage) consumer.receive(10000);
        assertNotNull(message);
        assertNull("JMS Message does not contain receipt request", message.getStringProperty(Stomp.Headers.RECEIPT_REQUESTED));

        frame = "DISCONNECT\n" + "\n\n" + Stomp.NULL;
        stompConnection.sendFrame(frame);
    }

    @Test
    public void testSubscribeWithAutoAck() throws Exception {

        String frame = "CONNECT\n" + "login:system\n" + "passcode:manager\n\n" + Stomp.NULL;
        stompConnection.sendFrame(frame);

        frame = stompConnection.receiveFrame();
        assertTrue(frame.startsWith("CONNECTED"));

        frame = "SUBSCRIBE\n" + "destination:/queue/" + getQueueName() + "\n" + "ack:auto\n\n" + Stomp.NULL;
        stompConnection.sendFrame(frame);

        sendMessage(name.getMethodName());

        frame = stompConnection.receiveFrame();
        assertTrue(frame.startsWith("MESSAGE"));

        frame = "DISCONNECT\n" + "\n\n" + Stomp.NULL;
        stompConnection.sendFrame(frame);
    }

    @Test
    public void testSubscribeWithAutoAckAndBytesMessage() throws Exception {

        String frame = "CONNECT\n" + "login:system\n" + "passcode:manager\n\n" + Stomp.NULL;
        stompConnection.sendFrame(frame);

        frame = stompConnection.receiveFrame();
        assertTrue(frame.startsWith("CONNECTED"));

        frame = "SUBSCRIBE\n" + "destination:/queue/" + getQueueName() + "\n" + "ack:auto\n\n" + Stomp.NULL;
        stompConnection.sendFrame(frame);

        sendBytesMessage(new byte[]{
                                       1, 2, 3, 4, 5
        });

        frame = stompConnection.receiveFrame();
        assertTrue(frame.startsWith("MESSAGE"));

        Pattern cl = Pattern.compile("Content-length:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher clMmatcher = cl.matcher(frame);
        assertTrue(clMmatcher.find());
        assertEquals("5", clMmatcher.group(1));

        assertFalse(Pattern.compile("type:\\s*null", Pattern.CASE_INSENSITIVE).matcher(frame).find());

        frame = "DISCONNECT\n" + "\n\n" + Stomp.NULL;
        stompConnection.sendFrame(frame);
    }

    @Test
    public void testBytesMessageWithNulls() throws Exception {

        String frame = "CONNECT\n" + "login:system\n" + "passcode:manager\n\n" + Stomp.NULL;
        stompConnection.sendFrame(frame);

        frame = stompConnection.receiveFrame();
        assertTrue(frame.startsWith("CONNECTED"));

        frame = "SEND\n" + "destination:/queue/" + getQueueName() + "\ncontent-length:5" + " \n\n" + "\u0001\u0002\u0000\u0004\u0005" + Stomp.NULL;
        stompConnection.sendFrame(frame);

        frame = "SUBSCRIBE\n" + "destination:/queue/" + getQueueName() + "\n" + "ack:auto\n\n" + Stomp.NULL;
        stompConnection.sendFrame(frame);

        StompFrame message = stompConnection.receive();
        assertTrue(message.getAction().startsWith("MESSAGE"));

        String length = message.getHeaders().get("content-length");
        assertEquals("5", length);

        assertEquals(5, message.getContent().length);

        frame = "DISCONNECT\n" + "\n\n" + Stomp.NULL;
        stompConnection.sendFrame(frame);
    }

    @Test
    public void testSendMultipleBytesMessages() throws Exception {

        final int MSG_COUNT = 50;

        String frame = "CONNECT\n" + "login:system\n" + "passcode:manager\n\n" + Stomp.NULL;
        stompConnection.sendFrame(frame);

        frame = stompConnection.receiveFrame();
        assertTrue(frame.startsWith("CONNECTED"));

        for (int ix = 0; ix < MSG_COUNT; ix++) {
            frame = "SEND\n" + "destination:/queue/" + getQueueName() + "\ncontent-length:5" + " \n\n" + "\u0001\u0002\u0000\u0004\u0005" + Stomp.NULL;
            stompConnection.sendFrame(frame);
        }

        frame = "SUBSCRIBE\n" + "destination:/queue/" + getQueueName() + "\n" + "ack:auto\n\n" + Stomp.NULL;
        stompConnection.sendFrame(frame);

        for (int ix = 0; ix < MSG_COUNT; ix++) {
            StompFrame message = stompConnection.receive();
            assertTrue(message.getAction().startsWith("MESSAGE"));

            String length = message.getHeaders().get("content-length");
            assertEquals("5", length);

            assertEquals(5, message.getContent().length);
        }

        frame = "DISCONNECT\n" + "\n\n" + Stomp.NULL;
        stompConnection.sendFrame(frame);
    }

    protected String getQueueName() {
        return getClass().getName() + "." + name.getMethodName();
    }

    protected String getTopicName() {
        return getClass().getName() + "." + name.getMethodName();
    }

    protected Socket createSocket() throws IOException {
        return new Socket("localhost", testProtocolServer.getBoundPort());
    }

    public void sendBytesMessage(byte[] msg) throws Exception {
        MessageProducer producer = session.createProducer(queue);
        BytesMessage message = session.createBytesMessage();
        message.writeBytes(msg);
        producer.send(message);
    }

    public void sendMessage(String msg) throws Exception {
        sendMessage(msg, "foo", "xyz");
    }

    public void sendMessage(String msg, String propertyName, String propertyValue) throws JMSException {
        MessageProducer producer = session.createProducer(queue);
        TextMessage message = session.createTextMessage(msg);
        message.setStringProperty(propertyName, propertyValue);
        producer.send(message);
    }

}
