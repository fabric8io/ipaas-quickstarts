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

package io.fabric8.mq.coordination;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.ReplicationController;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

public class ReplicationControllerTest {

    @Test
    public void testCreateReplicationController() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        /*
        String basedir = System.getProperty("basedir", "apps/fabric8-mq-controller");
        String fileName = basedir + "/src/main/resources/replication-template.json";
        */

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = classLoader.getResource("META-INF/replicator-template.json");

        ReplicationController result = mapper.reader(ReplicationController.class).readValue(url);

        Assert.assertNotNull(result);
    }
}
