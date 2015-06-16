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
package io.fabric8.mq.camel;

import org.apache.activemq.command.ActiveMQMessage;
import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.jms.JmsMessage;
import org.apache.camel.converter.ObjectConverter;
import org.apache.camel.impl.DefaultAsyncProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MQProducer extends DefaultAsyncProducer {
    private static Logger LOG = LoggerFactory.getLogger(MQProducer.class);
    private final MQEndpoint MQEndpoint;

    public MQProducer(MQEndpoint endpoint) {
        super(endpoint);
        MQEndpoint = endpoint;
    }

    @Override
    public boolean process(Exchange exchange, AsyncCallback callback) {
        try {
            //In the middle of the broker - InOut doesn't make any sense
            //so we do in only
            return processInOnly(exchange, callback);
        } catch (Throwable e) {
            // must catch exception to ensure callback is invoked as expected
            // to let Camel error handling deal with this
            exchange.setException(e);
            callback.done(true);
            return true;
        }
    }

    protected boolean processInOnly(final Exchange exchange, final AsyncCallback callback) {
        try {
            ActiveMQMessage message = getMessage(exchange);
            if (message != null) {
                MessageRouter messageRouter = getMessageRouter(exchange);
                if (messageRouter != null) {
                    message.setDestination(MQEndpoint.getDestination());
                    MQEndpoint.inject(messageRouter, message);
                } else {
                    LOG.error("Transport not set for " + exchange);
                }
            }
        } catch (Exception e) {
            exchange.setException(e);
        }
        callback.done(true);
        return true;
    }

    private MessageRouter getMessageRouter(Exchange exchange) throws Exception {
        Message camelMessage;
        if (exchange.hasOut()) {
            camelMessage = exchange.getOut();
        } else {
            camelMessage = exchange.getIn();
        }
        if (camelMessage instanceof MQJmsMessage) {
            return ((MQJmsMessage) camelMessage).getMessageRouter();
        }
        return null;
    }

    private ActiveMQMessage getMessage(Exchange exchange) throws Exception {
        ActiveMQMessage result;
        Message camelMessage;
        if (exchange.hasOut()) {
            camelMessage = exchange.getOut();
        } else {
            camelMessage = exchange.getIn();
        }

        Map<String, Object> headers = camelMessage.getHeaders();

        /**
         * We purposely don't want to support injecting messages half-way through
         * broker processing - use the activemq camel component for that - but
         * we will support changing message headers and destinations
         */
        if (camelMessage instanceof JmsMessage) {
            JmsMessage jmsMessage = (JmsMessage) camelMessage;
            if (jmsMessage.getJmsMessage() instanceof ActiveMQMessage) {
                result = (ActiveMQMessage) jmsMessage.getJmsMessage();
                //lets apply any new message headers
                setJmsHeaders(result, headers);
            } else {
                throw new IllegalStateException("Not the original message from the broker " + jmsMessage.getJmsMessage());
            }
        } else {
            throw new IllegalStateException("Not the original message from the broker " + camelMessage);
        }

        return result;
    }

    private void setJmsHeaders(ActiveMQMessage message, Map<String, Object> headers) {
        message.setReadOnlyProperties(false);
        for (Map.Entry<String, Object> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase("JMSDeliveryMode")) {
                Object value = entry.getValue();
                if (value instanceof Number) {
                    Number number = (Number) value;
                    message.setJMSDeliveryMode(number.intValue());
                }
            }
            if (entry.getKey().equalsIgnoreCase("JmsPriority")) {
                Integer value = ObjectConverter.toInteger(entry.getValue());
                if (value != null) {
                    message.setJMSPriority(value.intValue());
                }
            }
            if (entry.getKey().equalsIgnoreCase("JMSTimestamp")) {
                Long value = ObjectConverter.toLong(entry.getValue());
                if (value != null) {
                    message.setJMSTimestamp(value.longValue());
                }
            }
            if (entry.getKey().equalsIgnoreCase("JMSExpiration")) {
                Long value = ObjectConverter.toLong(entry.getValue());
                if (value != null) {
                    message.setJMSExpiration(value.longValue());
                }
            }
            if (entry.getKey().equalsIgnoreCase("JMSRedelivered")) {
                message.setJMSRedelivered(ObjectConverter.toBool(entry.getValue()));
            }
            if (entry.getKey().equalsIgnoreCase("JMSType")) {
                Object value = entry.getValue();
                if (value != null) {
                    message.setJMSType(value.toString());
                }
            }
        }
    }

}
