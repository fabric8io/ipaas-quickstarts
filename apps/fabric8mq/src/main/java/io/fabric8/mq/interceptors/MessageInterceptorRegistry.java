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
package io.fabric8.mq.interceptors;

import io.fabric8.mq.interceptors.camel.CommandMessage;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.Command;
import org.apache.activemq.filter.DestinationMap;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MessageInterceptorRegistry {

    private static final ConcurrentMap<String, MessageInterceptorRegistry> REGISTRY_MAP = new ConcurrentHashMap<>();
    private DestinationMap interceptorMap = new DestinationMap();

    private MessageInterceptorRegistry() {
    }

    public static MessageInterceptorRegistry getInstance(String name) {
        MessageInterceptorRegistry messageInterceptorRegistry = REGISTRY_MAP.get(name);
        if (messageInterceptorRegistry == null) {
            messageInterceptorRegistry = new MessageInterceptorRegistry();
            REGISTRY_MAP.putIfAbsent(name, messageInterceptorRegistry);
        }
        return messageInterceptorRegistry;
    }

    public boolean processCommand(MessageRouter messageRouter, Command command) {
        CommandMessage commandMessage = new CommandMessage(messageRouter, command);
        ActiveMQDestination destination = commandMessage.getDestination();
        if (destination != null) {
            Set<MessageInterceptor> set = this.interceptorMap.get(destination);
            if (set != null && !set.isEmpty()) {
                for (MessageInterceptor mi : set) {
                    mi.intercept(messageRouter, commandMessage);
                }
                return true;
            }
        }
        return false;
    }

    public MessageInterceptor addMessageInterceptor(String destinationName, MessageInterceptor messageInterceptor) {
        ActiveMQDestination activeMQDestination = ActiveMQDestination.createDestination(destinationName, ActiveMQDestination.QUEUE_TYPE);
        interceptorMap.put(activeMQDestination, messageInterceptor);
        return messageInterceptor;
    }

    public void removeMessageInterceptor(String destinationName, MessageInterceptor interceptor) {
        ActiveMQDestination activeMQDestination = ActiveMQDestination.createDestination(destinationName, ActiveMQDestination.QUEUE_TYPE);
        interceptorMap.remove(activeMQDestination, interceptor);
    }

    public MessageInterceptor addMessageInterceptorForQueue(String destinationName, MessageInterceptor messageInterceptor) {
        ActiveMQDestination activeMQDestination = ActiveMQDestination.createDestination(destinationName, ActiveMQDestination.QUEUE_TYPE);
        interceptorMap.put(activeMQDestination, messageInterceptor);
        return messageInterceptor;
    }

    public void removeMessageInterceptorForQueue(String destinationName, MessageInterceptor interceptor) {
        ActiveMQDestination activeMQDestination = ActiveMQDestination.createDestination(destinationName, ActiveMQDestination.QUEUE_TYPE);
        interceptorMap.remove(activeMQDestination, interceptor);
    }

    public MessageInterceptor addMessageInterceptorForTopic(String destinationName, MessageInterceptor messageInterceptor) {
        ActiveMQDestination activeMQDestination = ActiveMQDestination.createDestination(destinationName, ActiveMQDestination.TOPIC_TYPE);
        interceptorMap.put(activeMQDestination, messageInterceptor);
        return messageInterceptor;
    }

    public void removeMessageInterceptorForTopic(String destinationName, MessageInterceptor interceptor) {
        ActiveMQDestination activeMQDestination = ActiveMQDestination.createDestination(destinationName, ActiveMQDestination.TOPIC_TYPE);
        interceptorMap.remove(activeMQDestination, interceptor);
    }

    public MessageInterceptor addMessageInterceptor(ActiveMQDestination activeMQDestination, MessageInterceptor messageInterceptor) {
        interceptorMap.put(activeMQDestination, messageInterceptor);
        return messageInterceptor;
    }

    public void removeMessageInterceptor(ActiveMQDestination activeMQDestination, MessageInterceptor messageInterceptor) {
        interceptorMap.remove(activeMQDestination, messageInterceptor);
    }

}
