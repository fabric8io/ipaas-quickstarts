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

package io.fabric8.apps.hubot.letschat;

import io.fabric8.kubernetes.generator.annotation.KubernetesModelProcessor;
import io.fabric8.openshift.api.model.TemplateBuilder;

@KubernetesModelProcessor
public class HubotLetschatModelProcessor {
    
    private static final String NAME = "hubot-letschat";

    public void onTemplate(TemplateBuilder builder) {
        builder.addNewServiceObject()
                .withNewMetadata()
                .withName("hubot-jenkins-notifier")
                .addToLabels("component", NAME)
                .addToLabels("provider", "fabric8")
                .endMetadata()
                .withNewSpec()
                .addNewPort()
                .withProtocol("TCP")
                .withPort(5555)
                .withNewTargetPort(5555)
                .endPort()
                .addToSelector("component", NAME)
                .addToSelector("provider", "fabric8")
                .endSpec()
                .endServiceObject()
                .build();
    }
}
