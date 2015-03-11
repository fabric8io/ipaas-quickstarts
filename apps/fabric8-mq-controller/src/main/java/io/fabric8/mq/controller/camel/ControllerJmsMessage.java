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
package io.fabric8.mq.controller.camel;

import org.apache.camel.component.jms.JmsBinding;
import org.apache.camel.component.jms.JmsMessage;
import org.apache.camel.component.jms.JmsMessageHelper;
import org.apache.camel.util.ObjectHelper;

import javax.jms.Message;

public class ControllerJmsMessage extends JmsMessage {

    private MessageRouter messageRouter;

    public ControllerJmsMessage(MessageRouter messageRouter, Message jmsMessage, JmsBinding binding) {
        super(jmsMessage, binding);
        this.messageRouter = messageRouter;
    }

    public MessageRouter getMessageRouter() {
        return messageRouter;
    }

    public void setMessageRouter(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }

    @Override
    public String toString() {
        if (getJmsMessage() != null) {
            return "BrokerJmsMessage[JMSMessageID: " + JmsMessageHelper.getJMSMessageID(getJmsMessage());
        } else {
            return "BrokerJmsMessage@" + ObjectHelper.getIdentityHashCode(this);
        }
    }

    @Override
    public void copyFrom(org.apache.camel.Message that) {
        super.copyFrom(that);
        if (that instanceof JmsMessage) {
            JmsMessage thatJmsMessage = (JmsMessage) that;
            if (getJmsMessage() == null) {
                setJmsMessage(thatJmsMessage.getJmsMessage());
            }
        }
        if (that instanceof ControllerJmsMessage) {
            ControllerJmsMessage thatControllerJmsMessage = (ControllerJmsMessage) that;
            if (getMessageRouter() == null) {
                setMessageRouter(thatControllerJmsMessage.getMessageRouter());
            }
        }
    }

    @Override
    public ControllerJmsMessage newInstance() {
        return new ControllerJmsMessage(null, null, getBinding());
    }
}
