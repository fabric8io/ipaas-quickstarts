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
package io.fabric8.api.registry.rules;

/**
 */
public class CamelRestService {
    private String state;
    private String url;
    private String baseUrl;
    private String basePath;
    private String uriTemplate;
    private String method;
    private String consumes;
    private String produces;
    private String inType;
    private String outType;
    private String description;
    private String routeId;

    @Override
    public String toString() {
        return "CamelRestService{" +
                "basePath='" + basePath + '\'' +
                ", state='" + state + '\'' +
                ", url='" + url + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", uriTemplate='" + uriTemplate + '\'' +
                ", method='" + method + '\'' +
                ", consumes='" + consumes + '\'' +
                ", produces='" + produces + '\'' +
                ", inType='" + inType + '\'' +
                ", outType='" + outType + '\'' +
                ", description='" + description + '\'' +
                ", routeId='" + routeId + '\'' +
                '}';
    }

    /**
     * Gets the state of the REST service (started, stopped, etc)
     */
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    /**
     * Gets the absolute url to the REST service (baseUrl + uriTemplate)
     */
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Gets the base url to the REST service
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Gets the base path to the REST service
     */
    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    /**
     * Gets the uri template
     */
    public String getUriTemplate() {
        return uriTemplate;
    }

    public void setUriTemplate(String uriTemplate) {
        this.uriTemplate = uriTemplate;
    }

    /**
     * Gets the HTTP method (GET, POST, PUT etc)
     */
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Optional details about what media-types the REST service accepts
     */
    public String getConsumes() {
        return consumes;
    }

    public void setConsumes(String consumes) {
        this.consumes = consumes;
    }

    /**
     * Optional details about what media-types the REST service returns
     */
    public String getProduces() {
        return produces;
    }

    public void setProduces(String produces) {
        this.produces = produces;
    }

    /**
     * Optional detail about input binding to a FQN class name.
     * <p/>
     * If the input accepts a list, then <tt>List&lt;class name&gt;</tt> is enclosed the name.
     */
    public String getInType() {
        return inType;
    }

    public void setInType(String inType) {
        this.inType = inType;
    }

    /**
     * Optional detail about output binding to a FQN class name.
     * <p/>
     * If the output accepts a list, then <tt>List&lt;class name&gt;</tt> is enclosed the name.
     */
    public String getOutType() {
        return outType;
    }

    public void setOutType(String outType) {
        this.outType = outType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }
}
