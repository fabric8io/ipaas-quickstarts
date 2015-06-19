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

package io.fabric8.mq.interceptors.camel;

import io.fabric8.mq.interceptors.MessageRouter;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.Command;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.command.Message;
import org.apache.activemq.command.MessageAck;
import org.apache.activemq.command.MessageDispatch;
import org.apache.camel.impl.DefaultMessage;

import java.util.Map;

public class CommandMessage extends DefaultMessage {
    private final MessageRouter messageRouter;
    private final Command command;

    public CommandMessage(MessageRouter messageRouter, Command command) {
        this.messageRouter = messageRouter;
        this.command = command;
    }

    public Command getCommand() {
        return command;
    }

    public MessageRouter getMessageRouter() {
        return messageRouter;
    }

    public ActiveMQDestination getDestination() {
        if (command.isMessage()) {
            return ((Message) command).getDestination();
        }
        if (command.isMessageDispatch()) {
            return ((MessageDispatch) command).getDestination();
        }
        if (command.isMessageAck()) {
            return ((MessageAck) command).getDestination();
        }
        if (command instanceof ConsumerInfo) {
            return ((ConsumerInfo) command).getDestination();
        }
        return null;
    }

    public void setDestination(ActiveMQDestination destination) {
        if (command.isMessage()) {
            Message message = (Message) command;
            if (message.getOriginalDestination() == null) {
                message.setOriginalDestination(message.getDestination());
            }
            message.setDestination(destination);
        }
        if (command.isMessageDispatch()) {
            MessageDispatch messageDispatch = (MessageDispatch) command;
            Message message = messageDispatch.getMessage();
            if (message.getOriginalDestination() == null) {
                message.setOriginalDestination(message.getDestination());
            }
            message.setDestination(destination);
            messageDispatch.setDestination(destination);
        }
        if (command.isMessageAck()) {
            ((MessageAck) command).setDestination(destination);
        }
        if (command instanceof ConsumerInfo) {
            ((ConsumerInfo) command).setDestination(destination);
        }
    }

    protected void ensureInitialHeaders() {
        if (this.command != null && !this.hasPopulatedHeaders()) {
            super.setHeaders(this.createHeaders());
        }

    }

    protected void populateInitialHeaders(Map<String, Object> map) {
        Message message = null;
        if (command.isMessage()) {
            message = (Message) this.command;
        }
        if (command.isMessageDispatch()) {
            message = ((MessageDispatch) command).getMessage();
        }

        if (message != null) {
            map.put("JMSCorrelationID", message.getCorrelationId());
            map.put("JMSDestination", message.getDestination());
            map.put("JMSExpiration", message.getExpiration());
            map.put("JMSMessageID", message.getMessageId().toString());
            map.put("JMSPriority", message.getPriority());
            map.put("JMSRedelivered", message.isRedelivered());
            map.put("JMSTimestamp", message.getTimestamp());
            map.put("JMSReplyTo", message.getReplyTo());
            map.put("JMSType", message.getType());
            map.put("JMSXGroupID", message.getGroupID());
            map.put("JMSXGroupSeq", message.getGroupSequence());
            map.put("JMSXUserID", message.getUserID());
        }
    }
}
