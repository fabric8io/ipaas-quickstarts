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


import io.fabric8.cdi.ServiceConverters;
import io.fabric8.cdi.annotations.Service;
import io.fabric8.utils.Strings;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.deltaspike.core.api.config.ConfigProperty;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

public class CuratorFrameworkProducer {
    
    @Inject
    private RetryPolicy retryPolicy;

    @Inject
    private ACLProvider aclProvider;
    
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
    @New
    private ServiceConverters converters;
    
    @Produces
    @Service
    @Default
    public CuratorFramework create(InjectionPoint injectionPoint) {
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
        .connectString(converters.serviceToString(injectionPoint).substring(6))
        .retryPolicy(retryPolicy)
        .sessionTimeoutMs(sessionTimeoutMs)
                .connectionTimeoutMs(connectionTimeoutMs)
                .canBeReadOnly(canBeReadOnly)
                .aclProvider(aclProvider);
        
        if (Strings.isNotBlank(namespace)) {
            builder = builder.namespace(namespace);
        }
        
        if (Strings.isNotBlank(authScheme) && Strings.isNotBlank(authData)) {
            builder = builder.authorization(authScheme, authData.getBytes());
        }
        
        CuratorFramework curatorFramework =  builder.build();
        curatorFramework.start();
        return curatorFramework;
    }

    public void close(@Disposes CuratorFramework curatorFramework) {
        curatorFramework.close();
    }
}
