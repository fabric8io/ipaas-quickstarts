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

package io.fabric8.apps.elasticsearch;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.util.IntOrString;
import io.fabric8.kubernetes.generator.annotation.KubernetesModelProcessor;

@KubernetesModelProcessor
public class ElasticSearchModelProcessor {

    public void onList(KubernetesListBuilder builder) {
        builder.addNewServiceItem()
                .withNewMetadata()
                    .withName("elasticsearch-cluster")
                    .endMetadata()
                .withNewSpec()
                    .withPortalIP("None")
                    .addNewPort()
                        .withName("")
                        .withProtocol("TCP")
                        .withPort(9300)
                        .withTargetPort(new IntOrString(9300))
                    .endPort()
                .addToSelector("component", "elasticsearch")
                .addToSelector("provider", "fabric8")
                .endSpec()
                .and()
                .build();
    }

    public void on(PodTemplateSpecBuilder builder) {
        PodSpec podSpec = new PodSpecBuilder(builder.getSpec())
                .addNewContainer()
                .withName("logstash-template")
                .withImage("fabric8/elasticsearch-logstash-template:" + System.getProperty("project.version"))
                .endContainer()
                .build();

        builder.withSpec(podSpec);
    }
}
