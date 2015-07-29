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

package io.fabric8.apps.console;

import io.fabric8.kubernetes.generator.annotation.KubernetesModelProcessor;
import io.fabric8.openshift.api.model.TemplateBuilder;

import java.util.Arrays;

@KubernetesModelProcessor
public class ConsoleModelProcessor {

    public void onList(TemplateBuilder builder) {
        builder.addNewOAuthClientObject()
                .withNewMetadata()
                .withName("fabric8")
                .and()
                .withRedirectURIs(Arrays.asList(
                        "http://localhost:9090",
                        "http://localhost:2772",
                        "http://localhost:9000",
                        "http://fabric8.${DOMAIN}",
                        "https://fabric8.${DOMAIN}"
                )).and()
                .addNewServiceAccountObject()
                .withNewMetadata().withName("fabric8").endMetadata()
                .endServiceAccountObject()
                .build();
    }
}
