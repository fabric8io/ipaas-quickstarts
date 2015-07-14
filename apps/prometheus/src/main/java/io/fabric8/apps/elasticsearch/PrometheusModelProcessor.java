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

import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.generator.annotation.KubernetesModelProcessor;

@KubernetesModelProcessor
public class PrometheusModelProcessor {

    public void on(PodTemplateSpecBuilder builder) {
        PodSpec podSpec = new PodSpecBuilder(builder.getSpec())
                .addNewContainer()
                .withName("prometheus-k8s-watcher")
                .withImage(System.getProperty("fabric8.dockerPrefix") + System.getProperty("fabric8.dockerUser") + "prometheus-k8s-watcher:" + System.getProperty("project.version"))
                .addNewVolumeMount().withName("prometheus-targets").withMountPath("/etc/prometheus/targets.d").endVolumeMount()
                .addToArgs("-insecure=true").addToArgs("-master=https://kubernetes.default.svc.cluster.local").addToArgs("-nodes-file=/etc/prometheus/targets.d/nodes.yml")
                .endContainer()
                .build();

        builder.withSpec(podSpec);
    }

}
