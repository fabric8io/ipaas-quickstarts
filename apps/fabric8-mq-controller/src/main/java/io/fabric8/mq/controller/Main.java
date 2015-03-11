/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.mq.controller;

import io.fabric8.utils.Systems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String args[]) {
        try {
            MQController mqController = new MQController();

            String host = Systems.getEnvVarOrSystemProperty("HOST", mqController.getHost());
            if (host == null || host.trim().isEmpty()) {
                host = "0.0.0.0";
            }
            mqController.setHost(host);
            Number port = Systems.getEnvVarOrSystemProperty("AMQ_PORT", mqController.getPort());
            mqController.setPort(port.intValue());
            Number connectionTimeout = Systems.getEnvVarOrSystemProperty("CONNECTION_TIMEOUT", mqController.getConnectionTimeout());
            mqController.setConnectionTimeout(connectionTimeout.intValue());
            Number numberOfServers = Systems.getEnvVarOrSystemProperty("SERVER_NUMBER", mqController.getNumberOfServers());
            mqController.setNumberOfServers(numberOfServers.intValue());
            Number numberOfMultiplexers = Systems.getEnvVarOrSystemProperty("MULTIPLEXER_NUMBER", mqController.getNumberOfMultiplexers());
            mqController.setNumberOfMultiplexers(numberOfMultiplexers.intValue());
            mqController.start();
            LOG.info("Started Fabric8-MQ-Controller");

            waitUntilStop();
        } catch (Throwable e) {
            LOG.error("Failed to Start Fabric8-MQ-Controller", e);
        }
    }

    protected static void waitUntilStop() {
        Object lock = new Object();
        while (true) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }
}
