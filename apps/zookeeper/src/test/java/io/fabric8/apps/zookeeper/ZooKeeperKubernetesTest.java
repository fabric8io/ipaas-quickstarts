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

import io.fabric8.arquillian.kubernetes.Constants;
import io.fabric8.arquillian.kubernetes.Session;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.model.ReplicationControllerSchema;
import io.fabric8.kubernetes.api.model.ServiceSchema;
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

    @Test
    public void testZooKeeper() throws Exception {
        assertThat(client).replicationControllers().haveAtLeast(3, new Condition<ReplicationControllerSchema>() {
            @Override
            public boolean matches(ReplicationControllerSchema replicationControllerSchema) {
                return replicationControllerSchema.getId().startsWith("zookeeper-controller");
            }
        });

        assertThat(client).services().haveAtLeast(3, new Condition<ServiceSchema>() {
            @Override
            public boolean matches(ServiceSchema serviceSchema) {
                return serviceSchema.getId().startsWith("zk-peer");
            }
        });

        assertThat(client).services().haveAtLeast(3, new Condition<ServiceSchema>() {
            @Override
            public boolean matches(ServiceSchema serviceSchema) {
                return serviceSchema.getId().startsWith("zk-election");
            }
        });

        assertThat(client).pods().runningStatus().filterLabel(Constants.ARQ_KEY, session.getId()).hasSize(3);
    }
}
