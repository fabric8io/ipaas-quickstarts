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

package io.fabric8.tests.integration;

import io.fabric8.annotations.ServiceName;
import io.fabric8.arquillian.kubernetes.Session;
import io.fabric8.cdi.deltaspike.DeltaspikeTestBase;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.mq.ActiveMQConfigurer;
import io.fabric8.quickstarts.camelcdi.ActiveMQComponentFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.assertj.core.api.Condition;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import static io.fabric8.kubernetes.assertions.Assertions.assertThat;

@RunWith(Arquillian.class)
public class AppKubernetesTest {

    @ArquillianResource
    KubernetesClient client;

    @ArquillianResource
    Session session;

    @Inject
    @ServiceName("fabric8mq")
    ActiveMQComponent activeMQComponent;

    @Deployment
    public static WebArchive createDeployment() {
        return DeltaspikeTestBase.createDeployment()
                .addClasses(DeltaspikeTestBase.getDeltaSpikeHolders())
                .addClasses(ActiveMQComponentFactory.class, ActiveMQConfigurer.class);
    }


    @Test
    public void testAppProvisionsRunningPods() throws Exception {
        assertThat(client).pods()
                .runningStatus()
                .filterNamespace(session.getNamespace())
                .haveAtLeast(1, new Condition<Pod>() {
                    @Override
                    public boolean matches(Pod podSchema) {
                        return true;
                    }
                });
    }
}
