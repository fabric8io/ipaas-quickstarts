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

import org.apache.activemq.command.Message;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.component.jms.JmsBinding;
import org.apache.camel.impl.DefaultConsumer;

public class ControllerConsumer extends DefaultConsumer implements MessageInterceptor {
    private final JmsBinding jmsBinding = new JmsBinding();

    public ControllerConsumer(Endpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        ((ControllerEndpoint) getEndpoint()).addMessageInterceptor(this);
    }

    @Override
    protected void doStop() throws Exception {
        ((ControllerEndpoint) getEndpoint()).removeMessageInterceptor(this);
        super.doStop();
    }

    @Override
    public void intercept(MessageRouter messageRouter, Message message) {
        Exchange exchange = getEndpoint().createExchange(ExchangePattern.InOnly);

        exchange.setIn(new ControllerJmsMessage(messageRouter, (javax.jms.Message) message, jmsBinding));
        exchange.setProperty(Exchange.BINDING, jmsBinding);
        try {
            getProcessor().process(exchange);
        } catch (Exception e) {
            exchange.setException(e);
        }

        if (exchange.getException() != null) {
            getExceptionHandler().handleException("Error processing intercepted message: " + message, exchange, exchange.getException());
        }
    }

}
