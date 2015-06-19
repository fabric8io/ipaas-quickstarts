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
import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultAsyncProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MQProducer extends DefaultAsyncProducer {
    private static Logger LOG = LoggerFactory.getLogger(MQEndpoint.class);
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
            CommandMessage message = getMessage(exchange);
            if (message != null) {
                MessageRouter messageRouter = getMessageRouter(exchange);
                if (messageRouter != null) {
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
        if (camelMessage instanceof CommandMessage) {
            return ((CommandMessage) camelMessage).getMessageRouter();
        }
        return null;
    }

    private CommandMessage getMessage(Exchange exchange) throws Exception {
        CommandMessage result;
        Message camelMessage;
        if (exchange.hasOut()) {
            camelMessage = exchange.getOut();
        } else {
            camelMessage = exchange.getIn();
        }

        result = (CommandMessage) camelMessage;
        return result;
    }

}
