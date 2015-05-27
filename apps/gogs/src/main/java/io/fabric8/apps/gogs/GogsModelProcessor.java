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

package io.fabric8.apps.gogs;

import io.fabric8.kubernetes.generator.annotation.KubernetesModelProcessor;
import io.fabric8.openshift.api.model.OAuthClientBuilder;
import io.fabric8.openshift.api.model.template.TemplateBuilder;

import java.util.Arrays;

@KubernetesModelProcessor
public class GogsModelProcessor {
    
    private static final String NAME = "gogs";

    public void onList(TemplateBuilder builder) {
        builder.addNewOAuthClientObject()
                .withNewMetadata()
                .withName(NAME)
                .and()
                .withRedirectURIs(Arrays.asList(
                        "http://localhost:3000",
                        "http://gogs.${DOMAIN}",
                        "https://gogs.${DOMAIN}"))
                .and()
                .addNewServiceObject()
                .withNewMetadata()
                .withName(NAME + "-http-service")
                .addToLabels("component", NAME)
                .addToLabels("provider", "fabric8")
                .endMetadata()
                .withNewSpec()
                .addNewPort()
                .withProtocol("TCP")
                .withPort(80)
                .withNewTargetPort(3000)
                .endPort()
                .addToSelector("component", NAME)
                .addToSelector("provider", "fabric8")
                .endSpec()
                .endServiceObject()

                // Second service
                .addNewServiceObject()
                .withNewMetadata()
                .withName(NAME + "-ssh-service")
                .addToLabels("component", NAME)
                .addToLabels("provider", "fabric8")
                .endMetadata()
                .withNewSpec()
                .addNewPort()
                .withProtocol("TCP")
                .withPort(22)
                .withNewTargetPort(22)
                .endPort()
                .addToSelector("component", NAME)
                .addToSelector("provider", "fabric8")
                .endSpec()
                .endServiceObject()
                .build();
    }
}
