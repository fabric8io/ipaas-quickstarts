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

import org.apache.activemq.command.ActiveMQDestination;
import org.apache.camel.Endpoint;
import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.camel.impl.DefaultComponent;

import java.util.Map;

import static org.apache.camel.util.ObjectHelper.removeStartingCharacters;

/**
 * The Router Camel Component allows routing to be dynamically changed within the MQ Controller
 */
public class MQComponent extends DefaultComponent {

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {

        byte destinationType = ActiveMQDestination.QUEUE_TYPE;

        if (remaining.startsWith(JmsConfiguration.QUEUE_PREFIX)) {
            remaining = removeStartingCharacters(remaining.substring(JmsConfiguration.QUEUE_PREFIX.length()), '/');
        } else if (remaining.startsWith(JmsConfiguration.TOPIC_PREFIX)) {
            destinationType = ActiveMQDestination.TOPIC_TYPE;
            remaining = removeStartingCharacters(remaining.substring(JmsConfiguration.TOPIC_PREFIX.length()), '/');
        } else if (remaining.startsWith(JmsConfiguration.TEMP_QUEUE_PREFIX)) {
            destinationType = ActiveMQDestination.TEMP_QUEUE_TYPE;
            remaining = removeStartingCharacters(remaining.substring(JmsConfiguration.TEMP_QUEUE_PREFIX.length()), '/');
        } else if (remaining.startsWith(JmsConfiguration.TEMP_TOPIC_PREFIX)) {
            destinationType = ActiveMQDestination.TEMP_TOPIC_TYPE;
            remaining = removeStartingCharacters(remaining.substring(JmsConfiguration.TEMP_TOPIC_PREFIX.length()), '/');
        }

        ActiveMQDestination destination = ActiveMQDestination.createDestination(remaining, destinationType);
        MQEndpoint brokerEndpoint = new MQEndpoint(uri, this, destination);
        setProperties(brokerEndpoint, parameters);
        return brokerEndpoint;
    }
}
