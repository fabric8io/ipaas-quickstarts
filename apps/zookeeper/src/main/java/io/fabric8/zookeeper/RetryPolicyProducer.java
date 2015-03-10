/*
 * Copyright 2005-2014 Red Hat, Inc.
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
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.retry.RetryNTimes;
import org.apache.deltaspike.core.api.config.ConfigProperty;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class RetryPolicyProducer {
    
    public static final String NUMBER_OF_RETRIES = "NUMBER_OF_RETRIES";
    public static final String SLEEP_BETWEEN_RETRIES = "SLEEP_BETWEEN_RETRIES";
    public static final String EXPONENTIAL_BACKOFF_ENABLE = "EXPONENTIAL_BACKOFF_ENABLE";
    public static final String EXPONENTIAL_BACKOFF_BASE_SLEEP = "EXPONENTIAL_BACKOFF_BASE_SLEEP";
    public static final String EXPONENTIAL_BACKOFF_MAX_SLEEP = "EXPONENTIAL_BACKOFF_MAX_SLEEP";

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
    
    
    @Produces
    @Default
    RetryPolicy create(){
        if (exponentialBackOffEnabled) {
            return new ExponentialBackoffRetry(exponentialBaseSleep, numberOfRetries, exponentialMaxSleep);
        } else {
            return new RetryNTimes(numberOfRetries, sleepMsBetweenRetries);
        }
    }
    
}
