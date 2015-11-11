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

import org.apache.deltaspike.core.api.config.ConfigProperty;

import javax.inject.Inject;

public class ActiveMQConfig {
    
    public static final String DISPATCH_ASYNC = "DISPATCH_ASYNC";
    public static final String ALWAYS_SESSION_ASYNC = "ALWAYS_SESSION_ASYNC";
    public static final String OPTIMIZE_MESSAGE_DISPATCH = "OPTIMIZE_MESSAGE_DISPATCH";
    public static final String OPTIMIZE_ACKNOWLEDGE = "OPTIMIZE_ACKNOWLEDGE";
    public static final String OPTIMIZE_ACKNOWLEDGE_TIMEMOUT = "OPTIMIZE_ACKNOWLEDGE_TIMEMOUT";
    public static final String OPTIMIZED_ACK_SCHEDULED_ACK_INTERVAL = "OPTIMIZED_ACK_SCHEDULED_ACK_INTERVAL";
    public static final String COPY_MESSAGE_ON_SEND = "COPY_MESSAGE_ON_SEND";
    public static final String CLOSE_TIMEOUT = "CLOSE_TIMEOUT";
    public static final String NESTED_MAP_AND_LIST_ENABLED = "NESTED_MAP_AND_LIST_ENABLED";
    public static final String ALWAYS_SYNC_SEND = "ALWAYS_SYNC_SEND";
    private static final String WATCH_TOPIC_ADVISORIES = "WATCH_TOPIC_ADVISORIES";
    private static final String PRODUCER_WINDOW_SIZE = "PRODUCER_WINDOW_SIZE";
    private static final String WARN_ABOUT_UNSTARTED_CONNECTION_TIMEOUT = "WARN_ABOUT_UNSTARTED_CONNECTION_TIMEOUT";
    private static final String SEND_TIMEOUT = "SEND_TIMEOUT";
    private static final String SEND_ACKS_ASYNC = "SEND_ACKS_ASYNC";
    private static final String AUDIT_DEPTH = "AUDIT_DEPTH";
    private static final String AUDIT_MAX_PRODUCER_NUMBER = "AUDIT_MAX_PRODUCER_NUMBER";
    private static final String CONSUMER_FAILOVER_REDELIVERY_WAIT_PERIOD = "CONSUMER_FAILOVER_REDELIVERY_WAIT_PERIOD";
    private static final String CHECK_FOR_DUPLICATES = "CHECK_FOR_DUPLICATES";
    private static final String MESSAGE_PRIORITY_SUPPORTED = "MESSAGE_PRIORITY_SUPPORTED";
    private static final String TRANSACTED_IDIVIDUAL_ACK = "TRANSACTED_IDIVIDUAL_ACK";
    private static final String NON_BLOCKING_REDELIVERY = "NON_BLOCKING_REDELIVERY";
    private static final String MAX_THREAD_POOLSIZE = "MAX_THREAD_POOLSIZE";
    private static final String XA_ACK_MODE = "XA_ACK_MODE";
    private static final String RM_ID_FROM_CONNECTION_ID = "RM_ID_FROM_CONNECTION_ID";
    private static final String CONSUMER_EXPIRY_CHECK_ENABLED = "CONSUMER_EXPIRY_CHECK_ENABLED";
    private static final String DISABLE_TIMESTAPMS_BY_DEFAULT = "DISABLE_TIMESTAPMS_BY_DEFAULT";
    private static final String USE_ASYNC_SEND = "USE_ASYNC_SEND";
    private static final String USE_COMPRESSION = "USE_COMPRESSION";
    private static final String USERNAME="USERNAME";
    private static final String PASSWORD="PASSWORD";

    @Inject
    @ConfigProperty(name = DISPATCH_ASYNC, defaultValue = "true")
    private Boolean dispatchAsync;

    @Inject
    @ConfigProperty(name = ALWAYS_SESSION_ASYNC, defaultValue = "true")
    private Boolean alwaysSessionAsync;

    @Inject
    @ConfigProperty(name = OPTIMIZE_MESSAGE_DISPATCH, defaultValue = "true")
    private Boolean optimizeMessageDispatch;

    @Inject
    @ConfigProperty(name = OPTIMIZE_ACKNOWLEDGE, defaultValue = "false")
    private Boolean optimizeAcknowledge;

    @Inject
    @ConfigProperty(name = OPTIMIZE_ACKNOWLEDGE_TIMEMOUT, defaultValue = "300")
    private Long optimizeAcknowledgeTimeOut;

    @Inject
    @ConfigProperty(name = OPTIMIZED_ACK_SCHEDULED_ACK_INTERVAL, defaultValue = "0")
    private Long optimizedAckScheduledAckInterval;

    @Inject
    @ConfigProperty(name = COPY_MESSAGE_ON_SEND, defaultValue = "true")
    private Boolean copyMessageOnSend;

    @Inject
    @ConfigProperty(name = CLOSE_TIMEOUT, defaultValue = "1500")
    private Integer closeTiemout;

    @Inject
    @ConfigProperty(name = NESTED_MAP_AND_LIST_ENABLED, defaultValue = "true")
    private Boolean nestedMapAndListEnabled;

    @Inject
    @ConfigProperty(name = ALWAYS_SYNC_SEND, defaultValue = "false")
    private Boolean alwaysSyncSend;

    @Inject
    @ConfigProperty(name = WATCH_TOPIC_ADVISORIES, defaultValue = "true")
    private Boolean watchTopicAdvisories;

    @Inject
    @ConfigProperty(name = PRODUCER_WINDOW_SIZE, defaultValue = "0")
    private Integer producerWindowSize;

    @Inject
    @ConfigProperty(name = WARN_ABOUT_UNSTARTED_CONNECTION_TIMEOUT, defaultValue = "500")
    private Long warnAboutUnstartedConnectionTimeout;

    @Inject
    @ConfigProperty(name = SEND_TIMEOUT, defaultValue = "0")
    private Integer sendTimeout;

    @Inject
    @ConfigProperty(name = SEND_ACKS_ASYNC, defaultValue = "true")
    private Boolean sendAcsAsync;

    @Inject
    @ConfigProperty(name = AUDIT_DEPTH, defaultValue = "2048")
    private Long auditDepth;

    @Inject
    @ConfigProperty(name = AUDIT_MAX_PRODUCER_NUMBER, defaultValue = "64")
    private Integer auditMaxProducerNumber;

    @Inject
    @ConfigProperty(name = CONSUMER_FAILOVER_REDELIVERY_WAIT_PERIOD, defaultValue = "0")
    private Integer consumerFailoverRedeliveryWaitPeriod;

    @Inject
    @ConfigProperty(name = CHECK_FOR_DUPLICATES, defaultValue = "true")
    private Boolean checkForDuplicates;

    @Inject
    @ConfigProperty(name = MESSAGE_PRIORITY_SUPPORTED, defaultValue = "false")
    private Boolean messagePrioritySupported;

    @Inject
    @ConfigProperty(name = TRANSACTED_IDIVIDUAL_ACK, defaultValue = "false")
    private Boolean transactedIndividualAck;

    @Inject
    @ConfigProperty(name = NON_BLOCKING_REDELIVERY, defaultValue = "false")
    private Boolean nonBlockingRedelivery;

    @Inject
    @ConfigProperty(name = MAX_THREAD_POOLSIZE, defaultValue = "1000")
    private Integer maxThreadPoolSize;

    @Inject
    @ConfigProperty(name = XA_ACK_MODE, defaultValue = "-1")
    private Integer xaAckMode;

    @Inject
    @ConfigProperty(name = RM_ID_FROM_CONNECTION_ID, defaultValue = "false")
    private Boolean rmIdFromConnectionId;

    @Inject
    @ConfigProperty(name = CONSUMER_EXPIRY_CHECK_ENABLED, defaultValue = "false")
    private Boolean consumerExpiryCheckEnabled;

    @Inject
    @ConfigProperty(name = DISABLE_TIMESTAPMS_BY_DEFAULT, defaultValue = "false")
    private Boolean disableTimeStampsByDefault;

    @Inject
    @ConfigProperty(name = USE_ASYNC_SEND, defaultValue = "false")
    private Boolean useAsyncSend;

    @Inject
    @ConfigProperty(name = USE_COMPRESSION, defaultValue = "false")
    private Boolean useCompression;

    @Inject
    @ConfigProperty(name = USERNAME)
    private String username;

    @Inject
    @ConfigProperty(name = PASSWORD)
    private String password;

    public Boolean getDispatchAsync() {
        return dispatchAsync;
    }

    public Boolean getAlwaysSessionAsync() {
        return alwaysSessionAsync;
    }

    public Boolean getOptimizeMessageDispatch() {
        return optimizeMessageDispatch;
    }

    public Boolean getOptimizeAcknowledge() {
        return optimizeAcknowledge;
    }

    public Long getOptimizeAcknowledgeTimeOut() {
        return optimizeAcknowledgeTimeOut;
    }

    public Long getOptimizedAckScheduledAckInterval() {
        return optimizedAckScheduledAckInterval;
    }

    public Boolean getCopyMessageOnSend() {
        return copyMessageOnSend;
    }

    public Integer getCloseTiemout() {
        return closeTiemout;
    }

    public Boolean getNestedMapAndListEnabled() {
        return nestedMapAndListEnabled;
    }

    public Boolean getAlwaysSyncSend() {
        return alwaysSyncSend;
    }

    public Boolean getWatchTopicAdvisories() {
        return watchTopicAdvisories;
    }

    public Integer getProducerWindowSize() {
        return producerWindowSize;
    }

    public Long getWarnAboutUnstartedConnectionTimeout() {
        return warnAboutUnstartedConnectionTimeout;
    }

    public Integer getSendTimeout() {
        return sendTimeout;
    }

    public Boolean getSendAcsAsync() {
        return sendAcsAsync;
    }

    public Long getAuditDepth() {
        return auditDepth;
    }

    public Integer getAuditMaxProducerNumber() {
        return auditMaxProducerNumber;
    }

    public Integer getConsumerFailoverRedeliveryWaitPeriod() {
        return consumerFailoverRedeliveryWaitPeriod;
    }

    public Boolean getCheckForDuplicates() {
        return checkForDuplicates;
    }

    public Boolean getMessagePrioritySupported() {
        return messagePrioritySupported;
    }

    public Boolean getTransactedIndividualAck() {
        return transactedIndividualAck;
    }

    public Boolean getNonBlockingRedelivery() {
        return nonBlockingRedelivery;
    }

    public Integer getMaxThreadPoolSize() {
        return maxThreadPoolSize;
    }

    public Integer getXaAckMode() {
        return xaAckMode;
    }

    public Boolean getRmIdFromConnectionId() {
        return rmIdFromConnectionId;
    }

    public Boolean getConsumerExpiryCheckEnabled() {
        return consumerExpiryCheckEnabled;
    }

    public Boolean getDisableTimeStampsByDefault() {
        return disableTimeStampsByDefault;
    }

    public Boolean getUseAsyncSend() {
        return useAsyncSend;
    }

    public Boolean getUseCompression() {
        return useCompression;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
