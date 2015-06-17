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

package io.fabric8.apps.heapster;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.generator.annotation.KubernetesModelProcessor;

@KubernetesModelProcessor
public class HeapsterModelProcessor {

    public void on(ContainerBuilder builder) {
        builder.addToArgs(
                "-source=kubernetes:" + "" +
                "https://kubernetes.default.svc.cluster.local?" +
                "auth=&insecure=true&useServiceAccount=true")
                .addToArgs("-sink=influxdb:http://influxdb.default.svc.cluster.local:8086")
                .build();
    }
}
