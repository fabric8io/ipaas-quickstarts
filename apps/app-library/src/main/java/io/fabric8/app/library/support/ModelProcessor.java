/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.app.library.support;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.util.IntOrString;
import io.fabric8.kubernetes.generator.annotation.KubernetesModelProcessor;

import java.util.HashMap;
import java.util.Map;

/**
 * Adds an extra service as right now services with 2 ports are not
 * properly supported by the kubernetes REST /api/v1beta3/proxy/services API
 */
@KubernetesModelProcessor
public class ModelProcessor {
    public void onList(KubernetesListBuilder builder) {
        Map<String, String> labels = new HashMap<>();
        labels.put("component", "AppLibrary");
        labels.put("provider", "fabric8");

        builder.addToItems(new ServiceBuilder().
                withNewMetadata().
                withName("app-library-jolokia").
                withLabels(labels).
                endMetadata().
                withNewSpec().
                withSelector(labels).
                addNewPort().
                withPort(8778).
                withName("jolokia").
                withTargetPort(createIntOrString(8778)).
                endPort().
                endSpec().
                build()).
                build();
    }


    // TODO remove with KubernetesHelper.createIntOrString(intVal) when fabric8 2.1.2 or later is released!!!
    public static IntOrString createIntOrString(int intVal) {
        IntOrString answer = new IntOrString();
        answer.setIntVal(intVal);
        answer.setKind(KubernetesHelper.INTORSTRING_KIND_INT);
        return answer;
    }

}
