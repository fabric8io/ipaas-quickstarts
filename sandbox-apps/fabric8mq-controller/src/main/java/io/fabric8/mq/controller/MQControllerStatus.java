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
package io.fabric8.mq.controller;

import io.fabric8.mq.controller.camel.CamelRouter;
import org.apache.deltaspike.core.api.config.ConfigProperty;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicLong;

@Default
public class MQControllerStatus implements MQControllerStatusMBean {
    private final AtomicLong receivedConnectionAttempts = new AtomicLong();
    private final AtomicLong successfulConnectionAttempts = new AtomicLong();
    private final AtomicLong failedConnectionAttempts = new AtomicLong();
    @Inject
    @ConfigProperty(name = "INBOUND_CONNECTION_TIMOUT", defaultValue = "5000")
    private long connectionTimeout;
    @Inject
    @ConfigProperty(name = "NUMBER_NET_SERVERS", defaultValue = "-1")
    private int numberOfSevers;
    @Inject
    @ConfigProperty(name = "NUMBER_MULTIPLEXERS", defaultValue = "-1")
    private int numberOfMultiplexers;
    @Inject
    @ConfigProperty(name = "NAME", defaultValue = "MQController")
    private String name;
    @Inject
    @ConfigProperty(name = "CONTROLER_PORT", defaultValue = "6194")
    private int controllerPort;
    @Inject
    @ConfigProperty(name = "CONTROLER_HOST", defaultValue = "0.0.0.0")
    private String controllerHost;
    @Inject
    @ConfigProperty(name = "VIRTUAL_HOST", defaultValue = "")
    private String defaultVirtualHost;
    @Inject
    private CamelRouter camelRouter;

    private int boundPort;
    private String host;

    @Override
    public int getNumberOfMultiplexers() {
        return numberOfMultiplexers;
    }

    void setNumberOfMultiplexers(int numberOfMultiplexers) {
        this.numberOfMultiplexers = numberOfMultiplexers;
    }

    @Override
    public long getReceivedConnectionAttempts() {
        return receivedConnectionAttempts.get();
    }

    void incrementReceivedConnectionAttempts() {
        receivedConnectionAttempts.incrementAndGet();
    }

    @Override
    public long getSuccessfulConnectionAttempts() {
        return successfulConnectionAttempts.get();
    }

    void incrementSuccessfulConnectionAttempts() {
        successfulConnectionAttempts.incrementAndGet();
    }

    @Override
    public long getFailedConnectionAttempts() {
        return failedConnectionAttempts.get();
    }

    void incrementFailedConnectionAttempts() {
        failedConnectionAttempts.incrementAndGet();
    }

    @Override
    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    @Override
    public String getCamelRoutes() {
        return camelRouter.getCamelRoutes();
    }

    public void setCamelRoutes(String camelRoutes) {
        camelRouter.setCamelRoutes(camelRoutes);
    }

    @Override
    public int getNumberOfSevers() {
        return numberOfSevers;
    }

    void setNumberOfSevers(int numberOfSevers) {
        this.numberOfSevers = numberOfSevers;
    }

    @Override
    public String getDefaultVirtualHost() {
        return defaultVirtualHost;
    }

    void setDefaultVirtualHost(String defaultVirtualHost) {
        this.defaultVirtualHost = defaultVirtualHost;
    }

    @Override
    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getControllerHost() {
        return controllerHost;
    }

    void setControllerHost(String controllerHost) {
        this.controllerHost = controllerHost;
    }

    public int getControllerPort() {
        return controllerPort;
    }

    void setControllerPort(int controllerPort) {
        this.controllerPort = controllerPort;
    }

    @Override
    public int getBoundPort() {
        return boundPort;
    }

    void setBoundPort(int port) {
        boundPort = port;
    }

    @Override
    public String getHost() {
        return host;
    }

    void setHost(String host) {
        this.host = host;
    }

}
