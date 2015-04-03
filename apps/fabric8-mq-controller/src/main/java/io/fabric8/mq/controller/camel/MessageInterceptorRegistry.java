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

import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.Message;
import org.apache.activemq.filter.DestinationMap;

import java.util.Set;

public class MessageInterceptorRegistry {

    private static MessageInterceptorRegistry INSTANCE = new MessageInterceptorRegistry();
    private DestinationMap interceptorMap = new DestinationMap();

    private MessageInterceptorRegistry() {
    }

    public static MessageInterceptorRegistry getInstance() {
        return MessageInterceptorRegistry.INSTANCE;
    }

    public boolean processMessage(MessageRouter messageRouter, Message message) {
        ActiveMQDestination destination = message.getDestination();
        Set<MessageInterceptor> set = this.interceptorMap.get(destination);
        if (set != null && !set.isEmpty()) {
            Message copy = message.copy();
            for (MessageInterceptor mi : set) {
                mi.intercept(messageRouter, message);
            }
            return true;
        }
        return false;
    }

    MessageInterceptor addMessageInterceptor(String destinationName, MessageInterceptor messageInterceptor) {
        ActiveMQDestination activeMQDestination = ActiveMQDestination.createDestination(destinationName, ActiveMQDestination.QUEUE_TYPE);
        interceptorMap.put(activeMQDestination, messageInterceptor);
        return messageInterceptor;
    }

    void removeMessageInterceptor(String destinationName, MessageInterceptor interceptor) {
        ActiveMQDestination activeMQDestination = ActiveMQDestination.createDestination(destinationName, ActiveMQDestination.QUEUE_TYPE);
        interceptorMap.remove(activeMQDestination, interceptor);
    }

    MessageInterceptor addMessageInterceptorForQueue(String destinationName, MessageInterceptor messageInterceptor) {
        ActiveMQDestination activeMQDestination = ActiveMQDestination.createDestination(destinationName, ActiveMQDestination.QUEUE_TYPE);
        interceptorMap.put(activeMQDestination, messageInterceptor);
        return messageInterceptor;
    }

    void removeMessageInterceptorForQueue(String destinationName, MessageInterceptor interceptor) {
        ActiveMQDestination activeMQDestination = ActiveMQDestination.createDestination(destinationName, ActiveMQDestination.QUEUE_TYPE);
        interceptorMap.remove(activeMQDestination, interceptor);
    }

    MessageInterceptor addMessageInterceptorForTopic(String destinationName, MessageInterceptor messageInterceptor) {
        ActiveMQDestination activeMQDestination = ActiveMQDestination.createDestination(destinationName, ActiveMQDestination.TOPIC_TYPE);
        interceptorMap.put(activeMQDestination, messageInterceptor);
        return messageInterceptor;
    }

    void removeMessageInterceptorForTopic(String destinationName, MessageInterceptor interceptor) {
        ActiveMQDestination activeMQDestination = ActiveMQDestination.createDestination(destinationName, ActiveMQDestination.TOPIC_TYPE);
        interceptorMap.remove(activeMQDestination, interceptor);
    }

    MessageInterceptor addMessageInterceptor(ActiveMQDestination activeMQDestination, MessageInterceptor messageInterceptor) {
        interceptorMap.put(activeMQDestination, messageInterceptor);
        return messageInterceptor;
    }

    void removeMessageInterceptor(ActiveMQDestination activeMQDestination, MessageInterceptor interceptor) {
        interceptorMap.remove(activeMQDestination, interceptor);
    }

}
