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

import io.fabric8.mq.protocol.InactivityMonitor;
import org.apache.activemq.ActiveMQPrefetchPolicy;
import org.apache.activemq.command.*;
import org.apache.activemq.transport.mqtt.MQTTProtocolException;
import org.apache.activemq.transport.mqtt.MQTTProtocolSupport;
import org.apache.activemq.util.ByteArrayOutputStream;
import org.apache.activemq.util.ByteSequence;
import org.apache.activemq.util.IOExceptionSupport;
import org.apache.activemq.util.IdGenerator;
import org.apache.activemq.util.LRUCache;
import org.apache.activemq.util.LongSequenceGenerator;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.fusesource.mqtt.codec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Destination;
import javax.jms.InvalidClientIDException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.security.auth.login.CredentialException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class MQTTProtocolConverter {

    public static final String QOS_PROPERTY_NAME = "ActiveMQ.MQTT.QoS";
    public static final String RETAIN_PROPERTY = "ActiveMQ.Retain";
    public static final String RETAINED_PROPERTY = "ActiveMQ.Retained";
    static final int DEFAULT_CACHE_SIZE = 5000;
    private static final Logger LOG = LoggerFactory.getLogger(MQTTProtocolConverter.class);
    private static final IdGenerator CONNECTION_ID_GENERATOR = new IdGenerator();
    private static final MQTTFrame PING_RESP_FRAME = new PINGRESP().encode();
    private static final double MQTT_KEEP_ALIVE_GRACE_PERIOD = 0.5;
    protected final LongSequenceGenerator consumerIdGenerator = new LongSequenceGenerator();
    protected final ConcurrentHashMap<ConsumerId, MQTTSubscription> subscriptionsByConsumerId = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<String, MQTTSubscription> mqttSubscriptionByTopic = new ConcurrentHashMap<>();
    private final ConnectionId connectionId = new ConnectionId(CONNECTION_ID_GENERATOR.generateId());
    private final SessionId sessionId = new SessionId(connectionId, -1);
    private final ProducerId producerId = new ProducerId(sessionId, 1);
    private final LongSequenceGenerator publisherIdGenerator = new LongSequenceGenerator();
    private final ConcurrentHashMap<Integer, ResponseHandler> resposeHandlers = new ConcurrentHashMap<>();
    private final Map<String, ActiveMQDestination> activeMQDestinationMap = new LRUCache<>(DEFAULT_CACHE_SIZE);
    private final Map<Destination, String> mqttTopicMap = new LRUCache<>(DEFAULT_CACHE_SIZE);

    private final Map<Short, MessageAck> consumerAcks = new LRUCache<>(DEFAULT_CACHE_SIZE);
    private final Map<Short, PUBREC> publisherRecs = new LRUCache<>(DEFAULT_CACHE_SIZE);

    private final MQTTTransport mqttTransport;

    private final Object commnadIdMutex = new Object();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final ConnectionInfo connectionInfo = new ConnectionInfo();
    boolean willSent = false;
    private int lastCommandId;
    private CONNECT connect;
    private String clientId;
    private long defaultKeepAlive;
    private int activeMQSubscriptionPrefetch = -1;

    /*
     * Subscription strategy configuration element.
     *   > mqtt-default-subscriptions
     *   > mqtt-virtual-topic-subscriptions
     */
    private boolean publishDollarTopics;

    public MQTTProtocolConverter(MQTTTransport mqttTransport) {
        this.mqttTransport = mqttTransport;
        this.defaultKeepAlive = 0;
    }

    int generateCommandId() {
        synchronized (commnadIdMutex) {
            return lastCommandId++;
        }
    }

    public void sendToActiveMQ(Command command, ResponseHandler handler) {

        // Lets intercept message send requests..
        if (command instanceof ActiveMQMessage) {
            ActiveMQMessage msg = (ActiveMQMessage) command;

            if (!getPublishDollarTopics() && msg.getDestination().getPhysicalName().startsWith("$")) {
                // We don't allow users to send to $ prefixed topics to avoid failing MQTT 3.1.1
                // specification requirements for system assigned destinations.
                if (handler != null) {
                    try {
                        handler.onResponse(this, new Response());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return;
            }

        }

        command.setCommandId(generateCommandId());
        if (handler != null) {
            command.setResponseRequired(true);
            resposeHandlers.put(command.getCommandId(), handler);
        }
        getMQTTTransport().sendToActiveMQ(command);
    }

    void sendToMQTT(MQTTFrame frame) {
        try {
            mqttTransport.sendToMQTT(frame);
        } catch (IOException e) {
            LOG.warn("Failed to send frame " + frame, e);
        }
    }

    /**
     * Convert a MQTT command
     */
    public void onMQTTCommand(MQTTFrame frame) throws IOException, JMSException {
        switch (frame.messageType()) {
            case PINGREQ.TYPE:
                LOG.debug("Received a ping from client: " + getClientId());
                sendToMQTT(PING_RESP_FRAME);
                LOG.debug("Sent Ping Response to " + getClientId());
                break;
            case CONNECT.TYPE:
                CONNECT connect = new CONNECT().decode(frame);
                onMQTTConnect(connect);
                LOG.debug("MQTT Client {} connected. (version: {})", getClientId(), connect.version());
                break;
            case DISCONNECT.TYPE:
                LOG.debug("MQTT Client {} disconnecting", getClientId());
                onMQTTDisconnect();
                break;
            case SUBSCRIBE.TYPE:
                onSubscribe(new SUBSCRIBE().decode(frame));
                break;
            case UNSUBSCRIBE.TYPE:
                onUnSubscribe(new UNSUBSCRIBE().decode(frame));
                break;
            case PUBLISH.TYPE:
                onMQTTPublish(new PUBLISH().decode(frame));
                break;
            case PUBACK.TYPE:
                onMQTTPubAck(new PUBACK().decode(frame));
                break;
            case PUBREC.TYPE:
                onMQTTPubRec(new PUBREC().decode(frame));
                break;
            case PUBREL.TYPE:
                onMQTTPubRel(new PUBREL().decode(frame));
                break;
            case PUBCOMP.TYPE:
                onMQTTPubComp(new PUBCOMP().decode(frame));
                break;
            default:
                handleException(new MQTTProtocolException("Unknown MQTTFrame type: " + frame.messageType(), true), frame);
        }
    }

    void onMQTTConnect(final CONNECT connect) throws MQTTProtocolException {
        if (connected.get()) {
            throw new MQTTProtocolException("Already connected.");
        }
        this.connect = connect;

        String clientId = "";
        if (connect.clientId() != null) {
            clientId = connect.clientId().toString();
        }

        String userName = null;
        if (connect.userName() != null) {
            userName = connect.userName().toString();
        }
        String passswd = null;
        if (connect.password() != null) {
            passswd = connect.password().toString();
        }

        configureInactivityMonitor(connect.keepAlive());

        connectionInfo.setConnectionId(connectionId);
        if (clientId != null && !clientId.isEmpty()) {
            connectionInfo.setClientId(clientId);
        } else {
            // Clean Session MUST be set for 0 length Client Id
            if (!connect.cleanSession()) {
                CONNACK ack = new CONNACK();
                ack.code(CONNACK.Code.CONNECTION_REFUSED_IDENTIFIER_REJECTED);
                try {
                    getMQTTTransport().sendToMQTT(ack.encode());
                    getMQTTTransport().onException(IOExceptionSupport.create("Invalid Client ID", null));
                } catch (IOException e) {
                    getMQTTTransport().onException(IOExceptionSupport.create(e));
                }
                return;
            }
            connectionInfo.setClientId("" + connectionInfo.getConnectionId().toString());
        }

        connectionInfo.setResponseRequired(true);
        connectionInfo.setUserName(userName);
        connectionInfo.setPassword(passswd);

        FutureResponseHandler responseHandler = new FutureResponseHandler();
        sendToActiveMQ(connectionInfo, responseHandler);

        Response response = responseHandler.getResponse();
        try {
            if (response != null) {
                if (response.isException()) {
                    // If the connection attempt fails we close the socket.
                    Throwable exception = ((ExceptionResponse) response).getException();
                    //let the client know
                    CONNACK ack = new CONNACK();
                    if (exception instanceof InvalidClientIDException) {
                        ack.code(CONNACK.Code.CONNECTION_REFUSED_IDENTIFIER_REJECTED);
                    } else if (exception instanceof SecurityException) {
                        ack.code(CONNACK.Code.CONNECTION_REFUSED_NOT_AUTHORIZED);
                    } else if (exception instanceof CredentialException) {
                        ack.code(CONNACK.Code.CONNECTION_REFUSED_BAD_USERNAME_OR_PASSWORD);
                    } else {
                        ack.code(CONNACK.Code.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
                    }
                    getMQTTTransport().sendToMQTT(ack.encode());
                    getMQTTTransport().onException(IOExceptionSupport.create(exception));
                    return;
                }
            }
        } catch (IOException e) {
            LOG.error("failure to connect for " + connectionInfo, e);
            return;
        }
        final SessionInfo sessionInfo = new SessionInfo(sessionId);
        sendToActiveMQ(sessionInfo, null);
        final ProducerInfo producerInfo = new ProducerInfo(producerId);

        responseHandler = new FutureResponseHandler();
        sendToActiveMQ(producerInfo, responseHandler);

        response = responseHandler.getResponse();
        try {
            if (response != null) {
                if (response.isException()) {
                    // If the connection attempt fails we close the socket.
                    Throwable exception = ((ExceptionResponse) response).getException();
                    CONNACK ack = new CONNACK();
                    ack.code(CONNACK.Code.CONNECTION_REFUSED_BAD_USERNAME_OR_PASSWORD);
                    getMQTTTransport().sendToMQTT(ack.encode());
                    getMQTTTransport().onException(IOExceptionSupport.create(exception));
                    return;
                }
                CONNACK ack = new CONNACK();
                ack.code(CONNACK.Code.CONNECTION_ACCEPTED);
                connected.set(true);
                getMQTTTransport().sendToMQTT(ack.encode());

                if (connect.cleanSession()) {
                    MQTTPacketIdGenerator.stopClientSession(getClientId());
                } else {
                    MQTTPacketIdGenerator.startClientSession(getClientId());
                }
            }
        } catch (IOException e) {
            LOG.error("failure to connect for " + producerInfo, e);
        }
    }

    void onMQTTDisconnect() throws MQTTProtocolException {
        if (connected.get()) {
            connected.set(false);
            sendToActiveMQ(connectionInfo.createRemoveCommand(), null);
            sendToActiveMQ(new ShutdownInfo(), null);
        }
        stopTransport();
    }

    void onSubscribe(SUBSCRIBE command) throws MQTTProtocolException {
        checkConnected();
        LOG.trace("MQTT SUBSCRIBE message:{} client:{} connection:{}",
                     command.messageId(), clientId, connectionInfo.getConnectionId());
        Topic[] topics = command.topics();
        if (topics != null) {
            byte[] qos = new byte[topics.length];
            for (int i = 0; i < topics.length; i++) {
                try {
                    qos[i] = onSubscribe(topics[i]);
                } catch (IOException e) {
                    throw new MQTTProtocolException("Failed to process subscription request", true, e);
                }
            }
            SUBACK ack = new SUBACK();
            ack.messageId(command.messageId());
            ack.grantedQos(qos);
            try {
                getMQTTTransport().sendToMQTT(ack.encode());
            } catch (IOException e) {
                LOG.warn("Couldn't send SUBACK for " + command, e);
            }
        } else {
            LOG.warn("No topics defined for Subscription " + command);
        }
    }

    public void onUnSubscribe(UNSUBSCRIBE command) throws MQTTProtocolException {
        checkConnected();
        UTF8Buffer[] topics = command.topics();
        if (topics != null) {
            for (UTF8Buffer topic : topics) {
                try {
                    onUnSubscribe(topic.toString());
                } catch (IOException e) {
                    throw new MQTTProtocolException("Failed to process unsubscribe request", true, e);
                }
            }
        }
        UNSUBACK ack = new UNSUBACK();
        ack.messageId(command.messageId());
        sendToMQTT(ack.encode());
    }

    /**
     * Dispatch an ActiveMQ command
     */
    public void onActiveMQCommand(Command command) throws Exception {
        if (command.isResponse()) {
            Response response = (Response) command;
            ResponseHandler rh = resposeHandlers.remove(Integer.valueOf(response.getCorrelationId()));
            if (rh != null) {
                rh.onResponse(this, response);
            } else {
                // Pass down any unexpected errors. Should this close the connection?
                if (response.isException()) {
                    Throwable exception = ((ExceptionResponse) response).getException();
                    handleException(exception, null);
                }
            }
        } else if (command.isMessageDispatch()) {
            MessageDispatch md = (MessageDispatch) command;
            MQTTSubscription sub = subscriptionsByConsumerId.get(md.getConsumerId());
            if (sub != null) {
                MessageAck ack = sub.createMessageAck(md);
                PUBLISH publish = sub.createPublish((ActiveMQMessage) md.getMessage());
                switch (publish.qos()) {
                    case AT_LEAST_ONCE:
                    case EXACTLY_ONCE:
                        publish.dup(publish.dup() || md.getMessage().isRedelivered());
                    case AT_MOST_ONCE:
                }
                if (ack != null && sub.expectAck(publish)) {
                    synchronized (consumerAcks) {
                        consumerAcks.put(publish.messageId(), ack);
                    }
                }
                LOG.trace("MQTT Snd PUBLISH message:{} client:{} connection:{}",
                             publish.messageId(), clientId, connectionInfo.getConnectionId());
                getMQTTTransport().sendToMQTT(publish.encode());
                if (ack != null && !sub.expectAck(publish)) {
                    getMQTTTransport().sendToActiveMQ(ack);
                }
            }
        } else if (command.getDataStructureType() == ConnectionError.DATA_STRUCTURE_TYPE) {
            // Pass down any unexpected async errors. Should this close the connection?
            Throwable exception = ((ConnectionError) command).getException();
            handleException(exception, null);
        } else if (command.isBrokerInfo()) {
            //ignore
        } else {
            LOG.debug("Do not know how to process ActiveMQ Command {}", command);
        }
    }

    void onMQTTPublish(PUBLISH command) throws IOException, JMSException {
        checkConnected();
        LOG.trace("MQTT Rcv PUBLISH message:{} client:{} connection:{}",
                     command.messageId(), clientId, connectionInfo.getConnectionId());
        ActiveMQMessage message = convertMessage(command);
        message.setProducerId(producerId);
        message.onSend();
        sendToActiveMQ(message, createResponseHandler(command));
    }

    void onMQTTPubAck(PUBACK command) {
        short messageId = command.messageId();
        LOG.trace("MQTT Rcv PUBACK message:{} client:{} connection:{}",
                     messageId, clientId, connectionInfo.getConnectionId());
        MQTTPacketIdGenerator.ackPacketId(getClientId(), messageId);
        MessageAck ack;
        synchronized (consumerAcks) {
            ack = consumerAcks.remove(messageId);
        }
        if (ack != null) {
            getMQTTTransport().sendToActiveMQ(ack);
        }
    }

    void onMQTTPubRec(PUBREC commnand) {
        //from a subscriber - send a PUBREL in response
        PUBREL pubrel = new PUBREL();
        pubrel.messageId(commnand.messageId());
        sendToMQTT(pubrel.encode());
    }

    void onMQTTPubRel(PUBREL command) {
        PUBREC ack;
        synchronized (publisherRecs) {
            ack = publisherRecs.remove(command.messageId());
        }
        if (ack == null) {
            LOG.warn("Unknown PUBREL: {} received", command.messageId());
        }
        PUBCOMP pubcomp = new PUBCOMP();
        pubcomp.messageId(command.messageId());
        sendToMQTT(pubcomp.encode());
    }

    void onMQTTPubComp(PUBCOMP command) {
        short messageId = command.messageId();
        MQTTPacketIdGenerator.ackPacketId(getClientId(), messageId);
        MessageAck ack;
        synchronized (consumerAcks) {
            ack = consumerAcks.remove(messageId);
        }
        if (ack != null) {
            getMQTTTransport().sendToActiveMQ(ack);
        }
    }

    ActiveMQMessage convertMessage(PUBLISH command) throws JMSException {
        ActiveMQBytesMessage msg = new ActiveMQBytesMessage();

        msg.setProducerId(producerId);
        MessageId id = new MessageId(producerId, publisherIdGenerator.getNextSequenceId());
        msg.setMessageId(id);
        LOG.trace("MQTT-->ActiveMQ: MQTT_MSGID:{} client:{} connection:{} ActiveMQ_MSGID:{}",
                     command.messageId(), clientId, connectionInfo.getConnectionId(), msg.getMessageId());
        msg.setTimestamp(System.currentTimeMillis());
        msg.setPriority((byte) Message.DEFAULT_PRIORITY);
        msg.setPersistent(command.qos() != QoS.AT_MOST_ONCE && !command.retain());
        msg.setIntProperty(QOS_PROPERTY_NAME, command.qos().ordinal());
        if (command.retain()) {
            msg.setBooleanProperty(RETAIN_PROPERTY, true);
        }

        ActiveMQDestination destination;
        synchronized (activeMQDestinationMap) {
            destination = activeMQDestinationMap.get(command.topicName().toString());
            if (destination == null) {
                String topicName = MQTTProtocolSupport.convertMQTTToActiveMQ(command.topicName().toString());

                destination = new ActiveMQTopic(topicName);

                activeMQDestinationMap.put(command.topicName().toString(), destination);
            }
        }

        msg.setJMSDestination(destination);
        msg.writeBytes(command.payload().data, command.payload().offset, command.payload().length);
        return msg;
    }

    public PUBLISH convertMessage(ActiveMQMessage message) throws IOException, JMSException, DataFormatException {
        PUBLISH result = new PUBLISH();
        // packet id is set in MQTTSubscription
        QoS qoS;
        if (message.propertyExists(QOS_PROPERTY_NAME)) {
            int ordinal = message.getIntProperty(QOS_PROPERTY_NAME);
            qoS = QoS.values()[ordinal];

        } else {
            qoS = message.isPersistent() ? QoS.AT_MOST_ONCE : QoS.AT_LEAST_ONCE;
        }
        result.qos(qoS);
        if (message.getBooleanProperty(RETAINED_PROPERTY)) {
            result.retain(true);
        }

        String topicName;
        synchronized (mqttTopicMap) {
            topicName = mqttTopicMap.get(message.getJMSDestination());
            if (topicName == null) {
                String amqTopicName = message.getDestination().getPhysicalName();
                topicName = MQTTProtocolSupport.convertActiveMQToMQTT(amqTopicName);
                mqttTopicMap.put(message.getJMSDestination(), topicName);
            }
        }
        result.topicName(new UTF8Buffer(topicName));

        if (message.getDataStructureType() == ActiveMQTextMessage.DATA_STRUCTURE_TYPE) {
            ActiveMQTextMessage msg = (ActiveMQTextMessage) message.copy();
            msg.setReadOnlyBody(true);
            String messageText = msg.getText();
            if (messageText != null) {
                result.payload(new Buffer(messageText.getBytes("UTF-8")));
            }
        } else if (message.getDataStructureType() == ActiveMQBytesMessage.DATA_STRUCTURE_TYPE) {
            ActiveMQBytesMessage msg = (ActiveMQBytesMessage) message.copy();
            msg.setReadOnlyBody(true);
            byte[] data = new byte[(int) msg.getBodyLength()];
            msg.readBytes(data);
            result.payload(new Buffer(data));
        } else if (message.getDataStructureType() == ActiveMQMapMessage.DATA_STRUCTURE_TYPE) {
            ActiveMQMapMessage msg = (ActiveMQMapMessage) message.copy();
            msg.setReadOnlyBody(true);
            Map<String, Object> map = msg.getContentMap();
            if (map != null) {
                result.payload(new Buffer(map.toString().getBytes("UTF-8")));
            }
        } else {
            ByteSequence byteSequence = message.getContent();
            if (byteSequence != null && byteSequence.getLength() > 0) {
                if (message.isCompressed()) {
                    Inflater inflater = new Inflater();
                    inflater.setInput(byteSequence.data, byteSequence.offset, byteSequence.length);
                    byte[] data = new byte[4096];
                    int read;
                    ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
                    while ((read = inflater.inflate(data)) != 0) {
                        bytesOut.write(data, 0, read);
                    }
                    byteSequence = bytesOut.toByteSequence();
                    bytesOut.close();
                }
                result.payload(new Buffer(byteSequence.data, byteSequence.offset, byteSequence.length));
            }
        }
        LOG.trace("ActiveMQ-->MQTT:MQTT_MSGID:{} client:{} connection:{} ActiveMQ_MSGID:{}",
                     result.messageId(), clientId, connectionInfo.getConnectionId(), message.getMessageId());
        return result;
    }

    public MQTTTransport getMQTTTransport() {
        return mqttTransport;
    }

    public void onTransportError() {
        if (connect != null) {
            if (connected.get()) {
                if (connect.willTopic() != null && connect.willMessage() != null && !willSent) {
                    willSent = true;
                    try {
                        PUBLISH publish = new PUBLISH();
                        publish.topicName(connect.willTopic());
                        publish.qos(connect.willQos());
                        publish.messageId(MQTTPacketIdGenerator.getNextSequenceId(getClientId()));
                        publish.payload(connect.willMessage());
                        ActiveMQMessage message = convertMessage(publish);
                        message.setProducerId(producerId);
                        message.onSend();

                        sendToActiveMQ(message, null);
                    } catch (Exception e) {
                        LOG.warn("Failed to publish Will Message " + connect.willMessage());
                    }
                }
                // remove connection info
                sendToActiveMQ(connectionInfo.createRemoveCommand(), null);
            }
        }
    }

    void configureInactivityMonitor(short keepAliveSeconds) {
        InactivityMonitor monitor = getMQTTTransport().getInactivityMonitor();

        // If the user specifically shuts off the InactivityMonitor with transport.useInactivityMonitor=false,
        // then ignore configuring it because it won't exist
        if (monitor == null) {
            return;
        }

        // Client has sent a valid CONNECT frame, we can stop the connect checker.
        monitor.stopConnectionCheck();

        long keepAliveMS = keepAliveSeconds * 1000;

        LOG.debug("MQTT Client {} requests heart beat of {} ms", getClientId(), keepAliveMS);

        try {
            // if we have a default keep-alive value, and the client is trying to turn off keep-alive,

            // we'll observe the server-side configured default value (note, no grace period)
            if (keepAliveMS == 0 && defaultKeepAlive > 0) {
                keepAliveMS = defaultKeepAlive;
            }

            long readGracePeriod = (long) (keepAliveMS * MQTT_KEEP_ALIVE_GRACE_PERIOD);

            monitor.setReadCheckTime(keepAliveMS);
            monitor.startRead();

            LOG.debug("MQTT Client {} established heart beat of  {} ms ({} ms + {} ms grace period)",
                         getClientId(), keepAliveMS, keepAliveMS, readGracePeriod);
        } catch (Exception ex) {
            LOG.warn("Failed to start MQTT InactivityMonitor ", ex);
        }
    }

    void handleException(Throwable exception, MQTTFrame command) {
        LOG.warn("Exception occurred processing: \n" + command + ": " + exception.toString());
        LOG.debug("Exception detail", exception);

        if (connected.get() && connectionInfo != null) {
            connected.set(false);
            sendToActiveMQ(connectionInfo.createRemoveCommand(), null);
        }
        stopTransport();
    }

    void checkConnected() throws MQTTProtocolException {
        if (!connected.get()) {
            throw new MQTTProtocolException("Not connected.");
        }
    }

    private void stopTransport() {
        try {
            getMQTTTransport().stop();
        } catch (Throwable e) {
            LOG.debug("Failed to stop MQTT transport ", e);
        }
    }

    ResponseHandler createResponseHandler(final PUBLISH command) {
        if (command != null) {
            switch (command.qos()) {
                case AT_LEAST_ONCE:
                    return new ResponseHandler() {
                        @Override
                        public void onResponse(MQTTProtocolConverter converter, Response response) throws IOException {
                            if (response.isException()) {
                                LOG.warn("Failed to send MQTT Publish: ", command, ((ExceptionResponse) response).getException());
                            } else {
                                PUBACK ack = new PUBACK();
                                ack.messageId(command.messageId());
                                LOG.trace("MQTT Snd PUBACK message:{} client:{} connection:{}",
                                             command.messageId(), clientId, connectionInfo.getConnectionId());
                                converter.getMQTTTransport().sendToMQTT(ack.encode());
                            }
                        }
                    };
                case EXACTLY_ONCE:
                    return new ResponseHandler() {
                        @Override
                        public void onResponse(MQTTProtocolConverter converter, Response response) throws IOException {
                            if (response.isException()) {
                                LOG.warn("Failed to send MQTT Publish: ", command, ((ExceptionResponse) response).getException());
                            } else {
                                PUBREC ack = new PUBREC();
                                ack.messageId(command.messageId());
                                synchronized (publisherRecs) {
                                    publisherRecs.put(command.messageId(), ack);
                                }
                                LOG.trace("MQTT Snd PUBACK message:{} client:{} connection:{}",
                                             command.messageId(), clientId, connectionInfo.getConnectionId());
                                converter.getMQTTTransport().sendToMQTT(ack.encode());
                            }
                        }
                    };
                case AT_MOST_ONCE:
                    break;
            }
        }
        return null;
    }

    public long getDefaultKeepAlive() {
        return defaultKeepAlive;
    }

    /**
     * Set the default keep alive time (in milliseconds) that would be used if configured on server side
     * and the client sends a keep-alive value of 0 (zero) on a CONNECT frame
     *
     * @param keepAlive the keepAlive in milliseconds
     */
    public void setDefaultKeepAlive(long keepAlive) {
        this.defaultKeepAlive = keepAlive;
    }

    public int getActiveMQSubscriptionPrefetch() {
        return activeMQSubscriptionPrefetch;
    }

    /**
     * set the default prefetch size when mapping the MQTT subscription to an ActiveMQ one
     * The default = 1
     *
     * @param activeMQSubscriptionPrefetch set the prefetch for the corresponding ActiveMQ subscription
     */
    public void setActiveMQSubscriptionPrefetch(int activeMQSubscriptionPrefetch) {
        this.activeMQSubscriptionPrefetch = activeMQSubscriptionPrefetch;
    }

    public boolean getPublishDollarTopics() {
        return publishDollarTopics;
    }

    public void setPublishDollarTopics(boolean publishDollarTopics) {
        this.publishDollarTopics = publishDollarTopics;
    }

    public ConnectionId getConnectionId() {
        return connectionId;
    }

    public SessionId getSessionId() {
        return sessionId;
    }

    public boolean isCleanSession() {
        return this.connect.cleanSession();
    }

    public String getClientId() {
        if (clientId == null) {
            if (connect != null && connect.clientId() != null) {
                clientId = connect.clientId().toString();
            } else {
                clientId = "";
            }
        }
        return clientId;
    }

    private byte onSubscribe(String topicName, QoS requestedQoS) throws MQTTProtocolException {
        ActiveMQDestination destination = new ActiveMQTopic(MQTTProtocolSupport.convertMQTTToActiveMQ(topicName));

        ConsumerInfo consumerInfo = new ConsumerInfo(getNextConsumerId());
        consumerInfo.setDestination(destination);
        consumerInfo.setPrefetchSize(ActiveMQPrefetchPolicy.DEFAULT_TOPIC_PREFETCH);
        consumerInfo.setRetroactive(true);
        consumerInfo.setDispatchAsync(true);
        // create durable subscriptions only when clean session is false
        if (!isCleanSession() && getClientId() != null && requestedQoS.ordinal() >= QoS.AT_LEAST_ONCE.ordinal()) {
            consumerInfo.setSubscriptionName(requestedQoS + ":" + topicName);
            consumerInfo.setPrefetchSize(ActiveMQPrefetchPolicy.DEFAULT_DURABLE_TOPIC_PREFETCH);
        }

        if (getActiveMQSubscriptionPrefetch() > 0) {
            consumerInfo.setPrefetchSize(getActiveMQSubscriptionPrefetch());
        }

        return doSubscribe(consumerInfo, topicName, requestedQoS);
    }

    protected byte doSubscribe(ConsumerInfo consumerInfo, final String topicName, final QoS qoS) throws MQTTProtocolException {
        MQTTSubscription mqttSubscription = new MQTTSubscription(this, topicName, qoS, consumerInfo);
        this.subscriptionsByConsumerId.put(consumerInfo.getConsumerId(), mqttSubscription);
        this.mqttSubscriptionByTopic.put(topicName, mqttSubscription);
        final byte[] qos = new byte[]{(byte) -1};
        final FutureResponseHandler responseHandler = new FutureResponseHandler();
        sendToActiveMQ(consumerInfo, responseHandler);
        Response response = responseHandler.getResponse(5000);
        if (response != null) {
            if (response.isException()) {
                Throwable throwable = ((ExceptionResponse) response).getException();
                LOG.warn("Error subscribing to {}", topicName, throwable);
                qos[0] = -128;
            } else {
                qos[0] = (byte) qoS.ordinal();
            }
        } else {
            qos[0] = -128;
        }

        if (qos[0] == -128) {
            this.subscriptionsByConsumerId.remove(consumerInfo.getConsumerId());
            this.mqttSubscriptionByTopic.remove(topicName);
        }
        return qos[0];
    }

    private ConsumerId getNextConsumerId() {
        return new ConsumerId(getSessionId(), consumerIdGenerator.getNextSequenceId());
    }

    public void onUnSubscribe(String topicName) throws MQTTProtocolException {
        MQTTSubscription subscription = mqttSubscriptionByTopic.remove(topicName);
        if (subscription != null) {
            doUnSubscribe(subscription);

            // check if the durable sub also needs to be removed
            if (subscription.getConsumerInfo().getSubscriptionName() != null) {
                // also remove it from restored durable subscriptions set

                RemoveSubscriptionInfo rsi = new RemoveSubscriptionInfo();
                rsi.setConnectionId(getConnectionId());
                rsi.setSubscriptionName(subscription.getConsumerInfo().getSubscriptionName());
                rsi.setClientId(getClientId());
                sendToActiveMQ(rsi, null);
            }
        }
    }

    private void doUnSubscribe(MQTTSubscription subscription) {
        mqttSubscriptionByTopic.remove(subscription.getTopicName());
        ConsumerInfo info = subscription.getConsumerInfo();
        if (info != null) {
            subscriptionsByConsumerId.remove(info.getConsumerId());

            RemoveInfo removeInfo = info.createRemoveCommand();
            sendToActiveMQ(removeInfo, new ResponseHandler() {
                @Override
                public void onResponse(MQTTProtocolConverter converter, Response response) throws IOException {
                    // ignore failures..
                }
            });
        }
    }

    private byte onSubscribe(final Topic topic) throws MQTTProtocolException {

        final String destinationName = topic.name().toString();
        final QoS requestedQoS = topic.qos();

        final MQTTSubscription mqttSubscription = mqttSubscriptionByTopic.get(destinationName);
        if (mqttSubscription != null) {
            if (requestedQoS != mqttSubscription.getQoS()) {
                // remove old subscription as the QoS has changed
                onUnSubscribe(destinationName);
            } else {
                return (byte) requestedQoS.ordinal();
            }
        }

        try {
            return onSubscribe(destinationName, requestedQoS);
        } catch (IOException e) {
            throw new MQTTProtocolException("Failed while intercepting subscribe", true, e);
        }
    }
}
