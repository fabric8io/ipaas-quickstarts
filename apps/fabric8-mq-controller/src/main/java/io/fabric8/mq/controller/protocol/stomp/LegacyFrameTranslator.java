/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.mq.controller.protocol.stomp;

import io.fabric8.mq.controller.protocol.ProtocolException;
import org.apache.activemq.command.ActiveMQBytesMessage;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.activemq.transport.stomp.Stomp;
import org.apache.activemq.transport.stomp.StompFrame;
import org.apache.activemq.util.ByteArrayOutputStream;
import org.apache.activemq.util.ByteSequence;

import javax.jms.Destination;
import javax.jms.JMSException;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements ActiveMQ 4.0 translations
 */
public class LegacyFrameTranslator implements FrameTranslator {

    public ActiveMQMessage convertFrame(StompProtocolConverter converter, StompFrame command) throws JMSException, ProtocolException {
        final Map<?, ?> headers = command.getHeaders();
        final ActiveMQMessage msg;
        /*
         * To reduce the complexity of this method perhaps a Chain of Responsibility
         * would be a better implementation
         */
        if (headers.containsKey(Stomp.Headers.AMQ_MESSAGE_TYPE)) {
            String intendedType = (String) headers.get(Stomp.Headers.AMQ_MESSAGE_TYPE);
            if (intendedType.equalsIgnoreCase("text")) {
                ActiveMQTextMessage text = new ActiveMQTextMessage();
                try {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream(command.getContent().length + 4);
                    DataOutputStream data = new DataOutputStream(bytes);
                    data.writeInt(command.getContent().length);
                    data.write(command.getContent());
                    text.setContent(bytes.toByteSequence());
                    data.close();
                } catch (Throwable e) {
                    throw new ProtocolException("Text could not bet set: " + e, false, e);
                }
                msg = text;
            } else if (intendedType.equalsIgnoreCase("bytes")) {
                ActiveMQBytesMessage byteMessage = new ActiveMQBytesMessage();
                byteMessage.writeBytes(command.getContent());
                msg = byteMessage;
            } else {
                throw new ProtocolException("Unsupported message type '" + intendedType + "'", false);
            }
        } else if (headers.containsKey(Stomp.Headers.CONTENT_LENGTH)) {
            headers.remove(Stomp.Headers.CONTENT_LENGTH);
            ActiveMQBytesMessage bm = new ActiveMQBytesMessage();
            bm.writeBytes(command.getContent());
            msg = bm;
        } else {
            ActiveMQTextMessage text = new ActiveMQTextMessage();
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream(command.getContent().length + 4);
                DataOutputStream data = new DataOutputStream(bytes);
                data.writeInt(command.getContent().length);
                data.write(command.getContent());
                text.setContent(bytes.toByteSequence());
                data.close();
            } catch (Throwable e) {
                throw new ProtocolException("Text could not bet set: " + e, false, e);
            }
            msg = text;
        }
        Helper.copyStandardHeadersFromFrameToMessage(converter, command, msg, this);
        return msg;
    }

    public StompFrame convertMessage(StompProtocolConverter converter, ActiveMQMessage message) throws IOException, JMSException {
        StompFrame command = new StompFrame();
        command.setAction(Stomp.Responses.MESSAGE);
        Map<String, String> headers = new HashMap<String, String>(25);
        command.setHeaders(headers);

        FrameTranslator.Helper.copyStandardHeadersFromMessageToFrame(converter, message, command, this);

        if (message.getDataStructureType() == ActiveMQTextMessage.DATA_STRUCTURE_TYPE) {

            if (!message.isCompressed() && message.getContent() != null) {
                ByteSequence msgContent = message.getContent();
                if (msgContent.getLength() > 4) {
                    byte[] content = new byte[msgContent.getLength() - 4];
                    System.arraycopy(msgContent.data, 4, content, 0, content.length);
                    command.setContent(content);
                }
            } else {
                ActiveMQTextMessage msg = (ActiveMQTextMessage) message.copy();
                String messageText = msg.getText();
                if (messageText != null) {
                    command.setContent(msg.getText().getBytes("UTF-8"));
                }
            }

        } else if (message.getDataStructureType() == ActiveMQBytesMessage.DATA_STRUCTURE_TYPE) {

            ActiveMQBytesMessage msg = (ActiveMQBytesMessage) message.copy();
            msg.setReadOnlyBody(true);
            byte[] data = new byte[(int) msg.getBodyLength()];
            msg.readBytes(data);

            headers.put(Stomp.Headers.CONTENT_LENGTH, Integer.toString(data.length));
            command.setContent(data);
        }

        return command;
    }

    public String convertDestination(StompProtocolConverter converter, Destination d) {
        if (d == null) {
            return null;
        }
        ActiveMQDestination activeMQDestination = (ActiveMQDestination) d;
        String physicalName = activeMQDestination.getPhysicalName();

        String rc = converter.getCreatedTempDestinationName(activeMQDestination);
        if (rc != null) {
            return rc;
        }

        StringBuilder buffer = new StringBuilder();
        if (activeMQDestination.isQueue()) {
            if (activeMQDestination.isTemporary()) {
                buffer.append("/remote-temp-queue/");
            } else {
                buffer.append("/queue/");
            }
        } else {
            if (activeMQDestination.isTemporary()) {
                buffer.append("/remote-temp-topic/");
            } else {
                buffer.append("/topic/");
            }
        }
        buffer.append(physicalName);
        return buffer.toString();
    }

    public ActiveMQDestination convertDestination(StompProtocolConverter converter, String name, boolean forceFallback) throws ProtocolException {
        if (name == null) {
            return null;
        }

        // in case of space padding by a client we trim for the initial detection, on fallback use
        // the un-trimmed value.
        String originalName = name;
        name = name.trim();

        if (name.startsWith("/queue/")) {
            String qName = name.substring("/queue/".length(), name.length());
            return ActiveMQDestination.createDestination(qName, ActiveMQDestination.QUEUE_TYPE);
        } else if (name.startsWith("/topic/")) {
            String tName = name.substring("/topic/".length(), name.length());
            return ActiveMQDestination.createDestination(tName, ActiveMQDestination.TOPIC_TYPE);
        } else if (name.startsWith("/remote-temp-queue/")) {
            String tName = name.substring("/remote-temp-queue/".length(), name.length());
            return ActiveMQDestination.createDestination(tName, ActiveMQDestination.TEMP_QUEUE_TYPE);
        } else if (name.startsWith("/remote-temp-topic/")) {
            String tName = name.substring("/remote-temp-topic/".length(), name.length());
            return ActiveMQDestination.createDestination(tName, ActiveMQDestination.TEMP_TOPIC_TYPE);
        } else if (name.startsWith("/temp-queue/")) {
            return converter.createTempDestination(name, false);
        } else if (name.startsWith("/temp-topic/")) {
            return converter.createTempDestination(name, true);
        } else {
            if (forceFallback) {
                try {
                    ActiveMQDestination fallback = ActiveMQDestination.getUnresolvableDestinationTransformer().transform(originalName);
                    if (fallback != null) {
                        return fallback;
                    }
                } catch (JMSException e) {
                    throw new ProtocolException("Illegal destination name: [" + originalName + "] -- ActiveMQ STOMP destinations "
                                                    + "must begin with one of: /queue/ /topic/ /temp-queue/ /temp-topic/", false, e);
                }
            }
            throw new ProtocolException("Illegal destination name: [" + originalName + "] -- ActiveMQ STOMP destinations "
                                            + "must begin with one of: /queue/ /topic/ /temp-queue/ /temp-topic/");
        }
    }

}
