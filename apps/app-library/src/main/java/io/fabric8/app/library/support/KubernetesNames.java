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
package io.fabric8.app.library.support;

import io.fabric8.kubernetes.api.Config;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.utils.Strings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts the kubernetes names from a Kubernetes JSON or YAML file
 */
public class KubernetesNames {
    private List<String> serviceNames = new ArrayList<>();
    private List<String> replicationControllerNames = new ArrayList<>();

    public static KubernetesNames loadFile(File kubernetesJsonOrYamlFile) throws IOException {
        KubernetesNames answer = new KubernetesNames();
        if (kubernetesJsonOrYamlFile.isFile() && kubernetesJsonOrYamlFile.exists()) {
            Object value = KubernetesHelper.loadJson(kubernetesJsonOrYamlFile);
            if (value != null) {
                answer.addNamesFromDTO(value);
            }
        }
        return answer;
    }

    @Override
    public String toString() {
        return "KubernetesIds{" +
                "replicationControllerNames=" + replicationControllerNames +
                ", serviceNames=" + serviceNames +
                '}';
    }


    public void addNamesFromDTO(Object dto) throws IOException {
        if (dto instanceof ReplicationController) {
            addNames((ReplicationController) dto);
        } else if (dto instanceof Service) {
            addNames((Service) dto);
        } else if (dto instanceof Config) {
            Config config = (Config) dto;
            List<Object> entities = KubernetesHelper.getEntities(config);
            for (Object entity : entities) {
                addNamesFromDTO(entity);
            }
        }
    }

    public void addNames(ReplicationController dto) {
        addNameToList(KubernetesHelper.getId(dto), replicationControllerNames);
    }

    public void addNames(Service dto) {
        addNameToList(KubernetesHelper.getId(dto), serviceNames);
    }

    /**
     * Adds the name to the given list if its not blank or null and not already in the list
     */
    protected void addNameToList(String name, List<String> list) {
        if (list != null && Strings.isNotBlank(name) && !list.contains(name)) {
            list.add(name);
        }
    }

    public List<String> getReplicationControllerNames() {
        return replicationControllerNames;
    }

    public void setReplicationControllerNames(List<String> replicationControllerNames) {
        this.replicationControllerNames = replicationControllerNames;
    }

    public List<String> getServiceNames() {
        return serviceNames;
    }

    public void setServiceNames(List<String> serviceNames) {
        this.serviceNames = serviceNames;
    }
}
