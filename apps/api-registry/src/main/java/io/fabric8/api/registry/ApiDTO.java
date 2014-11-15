/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.api.registry;

import io.fabric8.kubernetes.api.model.ManifestContainer;
import io.fabric8.kubernetes.api.model.PodSchema;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

/**
 * Represents an API endpoint within a container
 */
@XmlRootElement(name = "api")
public class ApiDTO {
    private String podId;
    private Map<String, String> labels;
    private String containerName;
    private String objectName;
    private String url;
    private String state;
    private String jolokiaUrl;
    private String swaggerUrl;
    private String wadlUrl;
    private String wsdlUrl;

    public ApiDTO() {
    }

    public ApiDTO(String podId, Map<String, String> labels, String containerName, String objectName, String url, String state, String jolokiaUrl, String swaggerUrl, String wadlUrl, String wsdlUrl) {
        this.podId = podId;
        this.labels = labels;
        this.containerName = containerName;
        this.objectName = objectName;
        this.url = url;
        this.state = state;
        this.jolokiaUrl = jolokiaUrl;
        this.swaggerUrl = swaggerUrl;
        this.wsdlUrl = wsdlUrl;
        this.wadlUrl = wadlUrl;
    }

    public ApiDTO(PodSchema pod, ManifestContainer container, String objectName, String url, String state, String jolokiaUrl, String swaggerUrl, String wadlUrl, String wsdlUrl) {
        this(pod.getId(), pod.getLabels(), container.getName(), objectName, url, state, jolokiaUrl, swaggerUrl, wadlUrl, wsdlUrl);
    }

    @Override
    public String toString() {
        return "ApiDTO{" +
                "podId='" + podId + '\'' +
                ", labels=" + labels +
                ", containerName='" + containerName + '\'' +
                ", objectName='" + objectName + '\'' +
                ", url='" + url + '\'' +
                ", state='" + state + '\'' +
                ", jolokiaUrl='" + jolokiaUrl + '\'' +
                ", swaggerUrl='" + swaggerUrl + '\'' +
                ", wadlUrl='" + wadlUrl + '\'' +
                ", wsdlUrl='" + wsdlUrl + '\'' +
                '}';
    }

    public String getPodId() {
        return podId;
    }

    public void setPodId(String podId) {
        this.podId = podId;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getJolokiaUrl() {
        return jolokiaUrl;
    }

    public void setJolokiaUrl(String jolokiaUrl) {
        this.jolokiaUrl = jolokiaUrl;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public String getSwaggerUrl() {
        return swaggerUrl;
    }

    public void setSwaggerUrl(String swaggerUrl) {
        this.swaggerUrl = swaggerUrl;
    }

    public String getWsdlUrl() {
        return wsdlUrl;
    }

    public void setWsdlUrl(String wsdlUrl) {
        this.wsdlUrl = wsdlUrl;
    }

    public String getWadlUrl() {
        return wadlUrl;
    }

    public void setWadlUrl(String wadlUrl) {
        this.wadlUrl = wadlUrl;
    }
}
