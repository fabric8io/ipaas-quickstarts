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

package io.fabric8.apps.zookeeper;

import io.fabric8.arquillian.kubernetes.Session;
import io.fabric8.arquillian.kubernetes.annotation.Id;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.assertj.core.api.Condition;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;

import static io.fabric8.kubernetes.assertions.Assertions.assertThat;

@RunWith(Arquillian.class)
public class ZooKeeperKubernetesTest {

    @ArquillianResource
    KubernetesClient client;

    @ArquillianResource
    Session session;

    @Id("zk-client")
    @ArquillianResource
    Service zkClient;

    @Test
    public void testZooKeeper() throws Exception {
        assertThat(client).replicationControllers().haveAtLeast(3, new Condition<ReplicationController>() {
            @Override
            public boolean matches(ReplicationController replicationController) {
                return replicationController.getId().startsWith("zookeeper-controller");
            }
        });

        assertThat(client).services().haveAtLeast(3, new Condition<Service>() {
            @Override
            public boolean matches(Service serviceSchema) {
                return serviceSchema.getId().startsWith("zk-peer");
            }
        });

        assertThat(client).services().haveAtLeast(3, new Condition<Service>() {
            @Override
            public boolean matches(Service serviceSchema) {
                return serviceSchema.getId().startsWith("zk-election");
            }
        });

        assertThat(client).pods().runningStatus().filterNamespace(session.getNamespace()).hasSize(3);
    }

    @Test
    public void testClient() throws Exception {
        String serviceURL = zkClient.getPortalIP() + ":" + zkClient.getPort();
        try (CuratorFramework curator = CuratorFrameworkFactory.newClient(serviceURL, new RetryNTimes(5, 1000))) {
            curator.start();
            curator.blockUntilConnected();
            if (curator.checkExists().forPath("/fabric8") == null) {
                curator.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath("/fabric8");
            }
        }
    }

}
