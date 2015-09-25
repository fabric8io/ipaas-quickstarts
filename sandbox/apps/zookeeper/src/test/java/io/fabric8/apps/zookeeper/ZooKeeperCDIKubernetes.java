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

package io.fabric8.apps.zookeeper;

import io.fabric8.annotations.ServiceName;
import io.fabric8.cdi.deltaspike.DeltaspikeTestBase;
import io.fabric8.zookeeper.CuratorConfigurer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

@RunWith(Arquillian.class)
public class ZooKeeperCDIKubernetes {


    @Inject
    @ServiceName("zk-client")
    private CuratorFramework curator;


    @Deployment
    public static WebArchive createDeployment() {
        return DeltaspikeTestBase.createDeployment()
                .addClasses(DeltaspikeTestBase.getDeltaSpikeHolders())
                .addClasses(CuratorConfigurer.class);
    }

    @Test
    public void testClient() throws Exception {
        curator.blockUntilConnected();
        if (curator.checkExists().forPath("/fabric8") == null) {
            curator.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath("/fabric8");
        }
    }
}
