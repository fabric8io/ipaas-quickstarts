/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.cdelivery;

import io.fabric8.annotations.Protocol;
import io.fabric8.annotations.ServiceName;
import io.fabric8.utils.Strings;
import io.fabric8.utils.URLUtils;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.remote.client.api.RemoteRuntimeEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Produces;
import java.net.MalformedURLException;
import java.net.URL;

/**
 */
public class RemoteJbpmProducer {
    private static final transient Logger LOG = LoggerFactory.getLogger(RemoteJbpmProducer.class);

    @Produces
    public KieSession createKSession(@Protocol("http") @ServiceName("jbpm-designer") String jbpmDesignerServiceUrl,
                                     @ConfigProperty(name = "JBPM_CONTEXT", defaultValue = "jbpm-console") String context,
                                     @ConfigProperty(name = "JBPM_USERNAME", defaultValue = "krisv") String user,
                                     @ConfigProperty(name = "JBPM_PASSWORD", defaultValue = "krisv") String password,
                                     @ConfigProperty(name = "JBPM_DEPLOYMENT_ID", defaultValue = "demo") String deploymentId) throws MalformedURLException {

        if (Strings.isNullOrBlank(jbpmDesignerServiceUrl)) {
            jbpmDesignerServiceUrl = "http://localhost:8080/";
        }
        String url = jbpmDesignerServiceUrl;
        if (Strings.isNotBlank(context)) {
            url = URLUtils.pathJoin(jbpmDesignerServiceUrl, context);
        }
        LOG.info("Connecting to jBPM console at:  " + url + " as user: " + user + " with deploymentId: " + deploymentId);

        RuntimeEngine engine = RemoteRuntimeEngineFactory.newRestBuilder()
                .addUrl(new URL(url))
                .addUserName(user).addPassword(password)
                .addDeploymentId(deploymentId)
                .build();

        return engine.getKieSession();
    }
}
