/*
 * Copyright 2005-2015 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.quickstarts.camelcdi.mq;

import io.fabric8.annotations.Configuration;
import io.fabric8.annotations.Factory;
import io.fabric8.annotations.ServiceName;
import org.apache.activemq.ActiveMQConnectionFactory;

public class ActiveMQConfigurer {

    @Factory
    @ServiceName
    public ActiveMQConnectionFactory create(@ServiceName String service, @Configuration ActiveMQConfig config) {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(service);
        connectionFactory.setDispatchAsync(config.getDispatchAsync());
        connectionFactory.setAlwaysSessionAsync(config.getAlwaysSessionAsync());
        connectionFactory.setAlwaysSyncSend(config.getAlwaysSyncSend());
        connectionFactory.setAuditMaximumProducerNumber(config.getAuditMaxProducerNumber());
        connectionFactory.setCloseTimeout(config.getCloseTiemout());
        connectionFactory.setConsumerExpiryCheckEnabled(config.getConsumerExpiryCheckEnabled());
        connectionFactory.setCheckForDuplicates(config.getCheckForDuplicates());
        connectionFactory.setCopyMessageOnSend(config.getCopyMessageOnSend());
        connectionFactory.setDisableTimeStampsByDefault(config.getDisableTimeStampsByDefault());
        connectionFactory.setDispatchAsync(config.getDispatchAsync());
        connectionFactory.setMaxThreadPoolSize(config.getMaxThreadPoolSize());
        connectionFactory.setMessagePrioritySupported(config.getMessagePrioritySupported());
        connectionFactory.setNestedMapAndListEnabled(config.getNestedMapAndListEnabled());

        connectionFactory.setOptimizedMessageDispatch(config.getOptimizeMessageDispatch());
        connectionFactory.setOptimizeAcknowledgeTimeOut(config.getOptimizeAcknowledgeTimeOut());
        connectionFactory.setOptimizedAckScheduledAckInterval(config.getOptimizedAckScheduledAckInterval());
        connectionFactory.setOptimizeAcknowledge(config.getOptimizeAcknowledge());
        
        connectionFactory.setProducerWindowSize(config.getProducerWindowSize());
        connectionFactory.setSendAcksAsync(config.getSendAcsAsync());
        connectionFactory.setRmIdFromConnectionId(config.getRmIdFromConnectionId());
        connectionFactory.setSendTimeout(config.getSendTimeout());
        connectionFactory.setUseAsyncSend(config.getUseAsyncSend());
        connectionFactory.setUserName(config.getUsername());
        connectionFactory.setPassword(config.getPassword());
        connectionFactory.setUseCompression(config.getUseCompression());
        return connectionFactory;
    }
}
