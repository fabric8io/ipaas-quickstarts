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

import io.fabric8.annotations.Configuration;
import io.fabric8.annotations.Factory;
import io.fabric8.annotations.ServiceName;
import io.fabric8.utils.Strings;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;

public class CuratorConfigurer {

    @Factory
    @ServiceName
    public CuratorFramework create(@ServiceName String url, @Configuration CuratorConfig config) {
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString(url.substring(6))
                .retryPolicy(config.getRetryPolicy())
                .sessionTimeoutMs(config.getSessionTimeoutMs())
                .connectionTimeoutMs(config.getConnectionTimeoutMs())
                .canBeReadOnly(config.getCanBeReadOnly())
                .aclProvider(config.getAclProvider());

        if (Strings.isNotBlank(config.getNamespace())) {
            builder = builder.namespace(config.getNamespace());
        }

        if (Strings.isNotBlank(config.getAuthScheme()) && Strings.isNotBlank(config.getAuthData())) {
            builder = builder.authorization(config.getAuthScheme(), config.getAuthData().getBytes());
        }

        CuratorFramework curatorFramework = builder.build();
        curatorFramework.start();
        return curatorFramework;
    }
}
