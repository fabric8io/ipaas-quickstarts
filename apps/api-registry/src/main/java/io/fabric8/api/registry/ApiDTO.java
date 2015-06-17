/**
 *  Copyright 2005-2015 Red Hat, Inc.
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

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

import static io.fabric8.kubernetes.api.KubernetesHelper.getName;

/**
 * Represents an API endpoint within a container
 */
@XmlRootElement(name = "api")
public class ApiDTO {
    private String podId;
    private String serviceId;
    private Map<String, String> labels;
    private String containerName;
    private String objectName;
    private String path;
    private String url;
    private int port;
    private String state;
    private String jolokiaUrl;
    private String swaggerPath;
    private String swaggerUrl;
    private String wadlPath;
    private String wadlUrl;
    private String wsdlPath;
    private String wsdlUrl;

    public ApiDTO() {
    }

    public ApiDTO(String podId, String serviceId, Map<String, String> labels, String containerName, String objectName, String path, String url, int port, String state, String jolokiaUrl, String swaggerPath, String swaggerUrl, String wadlPath, String wadlUrl, String wsdlPath, String wsdlUrl) {
        this.podId = podId;
        this.serviceId = serviceId;
        this.labels = labels;
        this.containerName = containerName;
        this.objectName = objectName;
        this.url = url;
        this.path = path;
        this.port = port;
        this.state = state;
        this.jolokiaUrl = jolokiaUrl;
        this.swaggerPath = swaggerPath;
        this.swaggerUrl = swaggerUrl;
        this.wadlPath = wadlPath;
        this.wadlUrl = wadlUrl;
        this.wsdlPath = wsdlPath;
        this.wsdlUrl = wsdlUrl;
    }

    public ApiDTO(Pod pod, Container container, String serviceId, String objectName, String path, String url, int port, String state, String jolokiaUrl, String swaggerPath, String swaggerUrl, String wadlPath, String wadlUrl, String wsdlPath, String wsdlUrl) {
        this(getName(pod), serviceId, KubernetesHelper.getLabels(pod.getMetadata()), container.getName(), objectName, path, url, port, state, jolokiaUrl, swaggerPath, swaggerUrl, wadlPath, wadlUrl, wsdlPath, wsdlUrl);
    }

    @Override
    public String toString() {
        return "ApiDTO{" +
                "podId='" + podId + '\'' +
                ", serviceId='" + serviceId + '\'' +
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getSwaggerPath() {
        return swaggerPath;
    }

    public void setSwaggerPath(String swaggerPath) {
        this.swaggerPath = swaggerPath;
    }

    public String getWadlPath() {
        return wadlPath;
    }

    public void setWadlPath(String wadlPath) {
        this.wadlPath = wadlPath;
    }

    public String getWsdlPath() {
        return wsdlPath;
    }

    public void setWsdlPath(String wsdlPath) {
        this.wsdlPath = wsdlPath;
    }
}
