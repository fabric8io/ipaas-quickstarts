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

package io.fabric8.apps.infinispan;

import io.fabric8.arquillian.kubernetes.Session;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Asserts;
import io.fabric8.utils.Block;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.assertj.core.api.Condition;
import org.assertj.core.util.Preconditions;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

import static io.fabric8.kubernetes.api.KubernetesHelper.getName;
import static io.fabric8.kubernetes.assertions.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
public class InfinispanServerKubernetesTest {

    @ArquillianResource
    KubernetesClient client;

    @ArquillianResource
    Session session;

    @ArquillianResource
    ServiceList serviceList;

    @Test
    public void testInfinispan() throws Exception {
        assertThat(client).replicationControllers().haveAtLeast(1, new Condition<ReplicationController>() {
            @Override
            public boolean matches(ReplicationController replicationController) {
                return getName(replicationController).startsWith("infinispan");
            }
        });

        assertThat(client).services().haveAtLeast(1, new Condition<Service>() {
            @Override
            public boolean matches(Service serviceSchema) {
                return getName(serviceSchema).startsWith("infinispan-rest");
            }
        });

        assertThat(client).services().haveAtLeast(1, new Condition<Service>() {
            @Override
            public boolean matches(Service serviceSchema) {
                return getName(serviceSchema).startsWith("infinispan-remote");
            }
        });

        assertThat(client).services().haveAtLeast(1, new Condition<Service>() {
            @Override
            public boolean matches(Service serviceSchema) {
                return getName(serviceSchema).startsWith("infinispan-memcached");
            }
        });

        assertThat(client).services().haveAtLeast(1, new Condition<Service>() {
            @Override
            public boolean matches(Service serviceSchema) {
                return getName(serviceSchema).startsWith("infinispan-hotrod");
            }
        });

    }

    @Test
    public void testRestEndpoint() throws Exception {
        final String key = "key";
        final String expectedValue = "value1";

        final ServiceSpec restService = getRequiredServiceSpec("infinispan-rest");

        List<ServicePort> ports = restService.getPorts();
        assertTrue("Should have at least one port for service " + restService, ports.size() > 0);
        ServicePort firstServicePort = ports.get(0);

        final String serverURL = "http://" + restService.getClusterIP() + ":" + firstServicePort.getPort() + "/rest/default";
        //TODO: We need to find a more elegant/robust way to know when the service is actually ready.
        Asserts.assertWaitFor(2 * 60 * 1000, new Block() {
            @Override
            public void invoke() throws Exception {
                String actualValue = null;
                try {
                    putMethod(serverURL, key, expectedValue);
                    actualValue = getMethod(serverURL, key);
                } catch (Exception e) {
                    //ignore
                } finally {
                    Assert.assertEquals(expectedValue, actualValue);
                }
            }
        });
    }


    @Test
    public void testCluster() throws Exception {
        final JChannel channel = new JChannel(getClass().getResource("/jgroups.xml"));
        channel.connect("infinispan");
        Asserts.assertWaitFor(2 * 60 * 1000, new Block() {
            @Override
            public void invoke() throws Exception {
                for (Address address : channel.getView().getMembers()) {
                    System.out.println(address);
                }
                assertTrue(channel.getView().getMembers().size() > 3);
            }
        });
    }


    /**
     * Get the {@link io.fabric8.kubernetes.api.model.Service} with the specified id.
     * @param id    The id.
     * @return
     */
    private ServiceSpec getRequiredServiceSpec(String id) {
        Preconditions.checkNotNullOrEmpty(id);
        for (Service s : serviceList.getItems()) {
            if (id.equals(getName(s))) {
                ServiceSpec spec = s.getSpec();
                if (spec != null) {
                    return spec;
                }
            }
        }
        throw new IllegalStateException("Service with id:"+id+" doesn't exists.");
    }

    /**
     * Method that puts a String value in cache.
     */
    public static void putMethod(String serverURL, String key, String value) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPut put = new HttpPut(serverURL + "/"+ key);
        put.addHeader(new BasicHeader("Content-Type", "text/plain"));
        put.setEntity(new StringEntity(value));
        client.execute(put);
    }

    /**
     * Method that gets a value by a key in url as param value.
     */
    private static String getMethod(String serverURL, String key) throws IOException {
        return Request.Get(serverURL + "/" + key)
                .addHeader("Content-Type", "text/plain")
                .execute()
                .returnContent().asString();
    }
}
