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

import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.camel.Consumer;
import org.apache.camel.MultipleConsumersSupport;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.Service;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriPath;
import org.apache.camel.util.UnsafeUriCharactersEncoder;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@UriEndpoint(scheme = "controller", syntax = "controller:name", consumerClass = MQConsumer.class, label = "api", title = "Controller")
public class MQEndpoint extends DefaultEndpoint implements MultipleConsumersSupport, Service {

    @UriPath
    private final ActiveMQDestination destination;
    private MessageInterceptorRegistry messageInterceptorRegistry;
    private List<MessageInterceptor> messageInterceptorList = new CopyOnWriteArrayList<>();

    public MQEndpoint(String uri, MQComponent component, ActiveMQDestination destination) {
        super(UnsafeUriCharactersEncoder.encode(uri), component);
        this.destination = destination;
    }

    @Override
    public Producer createProducer() throws Exception {
        return new MQProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        MQConsumer consumer = new MQConsumer(this, processor);
        configureConsumer(consumer);
        return consumer;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public boolean isMultipleConsumersSupported() {
        return true;
    }

    public ActiveMQDestination getDestination() {
        return destination;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        messageInterceptorRegistry = MessageInterceptorRegistry.getInstance();
        for (MessageInterceptor messageInterceptor : messageInterceptorList) {
            addMessageInterceptor(messageInterceptor);
        }
        messageInterceptorList.clear();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
    }

    protected void addMessageInterceptor(MessageInterceptor messageInterceptor) {
        if (isStarted()) {
            messageInterceptorRegistry.addMessageInterceptor(destination, messageInterceptor);
        } else {
            messageInterceptorList.add(messageInterceptor);
        }
    }

    protected void removeMessageInterceptor(MessageInterceptor messageInterceptor) {
        messageInterceptorRegistry.removeMessageInterceptor(destination, messageInterceptor);
    }

    protected void inject(MessageRouter messageRouter, ActiveMQMessage message) throws Exception {
        if (message != null) {
            message.setDestination(destination);
            messageRouter.inject(message);
        }
    }
}
