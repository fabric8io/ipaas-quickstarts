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

package io.fabric8.zookeeper;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.imps.DefaultACLProvider;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.retry.RetryNTimes;
import org.apache.deltaspike.core.api.config.ConfigProperty;

import javax.inject.Inject;

public class CuratorConfig {

    @Inject
    @ConfigProperty(name = "SESSION_TIMEOUT_MS", defaultValue = "60000")
    private Integer sessionTimeoutMs;

    @Inject
    @ConfigProperty(name = "CONNECTION_TIMEOUT_MS", defaultValue = "60000")
    private Integer connectionTimeoutMs;

    @Inject
    @ConfigProperty(name = "CAN_BE_READONLY", defaultValue = "false")
    private Boolean canBeReadOnly;

    @Inject
    @ConfigProperty(name = "NAMESPACE")
    private String namespace;

    @Inject
    @ConfigProperty(name = "AUTH_SCHEME")
    private String authScheme;

    @Inject
    @ConfigProperty(name = "AUTH_DATA")
    private String authData;

    @Inject
    @ConfigProperty(name = "NUMBER_OF_RETRIES", defaultValue = "3")
    private Integer numberOfRetries;

    @Inject
    @ConfigProperty(name = "SLEEP_BETWEEN_RETRIES", defaultValue = "1000")
    private Integer sleepMsBetweenRetries;

    @Inject
    @ConfigProperty(name = "EXPONENTIAL_BACKOFF_ENABLE", defaultValue = "false")
    private Boolean exponentialBackOffEnabled;

    @Inject
    @ConfigProperty(name = "EXPONENTIAL_BACKOFF_BASE_SLEEP", defaultValue = "1000")
    private Integer exponentialBaseSleep;

    @Inject
    @ConfigProperty(name = "EXPONENTIAL_BACKOFF_MAX_SLEEP", defaultValue = "10000")
    private Integer exponentialMaxSleep;

    private ACLProvider aclProvider = new DefaultACLProvider();



    public Integer getSessionTimeoutMs() {
        return sessionTimeoutMs;
    }

    public Integer getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public Boolean getCanBeReadOnly() {
        return canBeReadOnly;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getAuthScheme() {
        return authScheme;
    }

    public String getAuthData() {
        return authData;
    }

    public ACLProvider getAclProvider() {
        return aclProvider;
    }
    
    public RetryPolicy getRetryPolicy() {
        if (exponentialBackOffEnabled) {
            return new ExponentialBackoffRetry(exponentialBaseSleep, numberOfRetries, exponentialMaxSleep);
        } else {
            return new RetryNTimes(numberOfRetries, sleepMsBetweenRetries);
        }
    }
}
