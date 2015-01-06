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
package io.fabric8.api.registry;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.swagger.model.ApiDeclaration;
import org.apache.cxf.jaxrs.ext.MessageContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a snapshot of the current Pod and API state.
 */
public class ApiSnapshot {
    private final Map<String, Pod> podMap;
    private String urlPrefix;
    private final Map<String, List<ApiDTO>> apiMap = new HashMap<>();
    private final Map<PodAndContainerId, ApiDeclaration> podContainerToSwaggerMap = new HashMap<>();
    private MessageContext messageContext;
    private List<ApiDTO> serviceApis = new ArrayList<>();

    public ApiSnapshot(Map<String, Pod> podMap) {
        this.podMap = podMap;
    }

    public Map<String, Pod> getPodMap() {
        return podMap;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public void setUrlPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;
    }

    public Map<String, List<ApiDTO>> getApiMap() {
        return apiMap;
    }

    public Map<PodAndContainerId, ApiDeclaration> getPodContainerToSwaggerMap() {
        return podContainerToSwaggerMap;
    }

    public void putApis(String podId, List<ApiDTO> list) {
        apiMap.put(podId, list);
    }

    public ApiDeclaration getSwaggerForPodAndContainer(PodAndContainerId key) {
        return podContainerToSwaggerMap.get(key);
    }

    public void putPodAndContainerSwagger(PodAndContainerId key, ApiDeclaration swagger) {
        podContainerToSwaggerMap.put(key, swagger);
    }

    public void setMessageContext(MessageContext messageContext) {
        this.messageContext = messageContext;
    }

    public MessageContext getMessageContext() {
        return messageContext;
    }

    public List<ApiDTO> getServiceApis() {
        return serviceApis;
    }

    public void addServiceApi(ApiDTO apiDTO) {
        serviceApis.add(apiDTO);
    }
}
