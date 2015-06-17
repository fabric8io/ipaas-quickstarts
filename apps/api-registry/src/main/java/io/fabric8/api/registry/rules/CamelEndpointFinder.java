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

import io.fabric8.api.registry.ApiDTO;
import io.fabric8.api.registry.ApiSnapshot;
import io.fabric8.api.registry.PodAndContainerId;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.swagger.model.Api;
import io.fabric8.swagger.model.ApiDeclaration;
import io.fabric8.swagger.model.Models;
import io.fabric8.swagger.model.Operation;
import io.fabric8.swagger.model.Parameter;
import io.fabric8.swagger.model.ResponseMessage;
import io.fabric8.utils.Strings;
import org.apache.commons.beanutils.BeanUtils;
import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static io.fabric8.api.registry.ApiFinder.getHttpUrl;
import static io.fabric8.kubernetes.api.KubernetesHelper.getName;
import static io.fabric8.utils.URLUtils.urlPathJoin;


/**
 * Rule to find <a href="http://camel.apache.org/">Apache Camel</a> REST endpoints via JMX
 */
public class CamelEndpointFinder extends EndpointFinderSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(CamelEndpointFinder.class);

    public static final String CAMEL_API_ENDPOINT_MBEAN_NAME = "org.apache.camel:type=services,name=DefaultRestRegistry,*";
    private boolean replaceWithProxyLink = false;

    @Override
    protected String getObjectNamePattern() {
        return CAMEL_API_ENDPOINT_MBEAN_NAME;
    }

    @Override
    protected void appendObjectNameEndpoints(List<ApiDTO> list, ApiSnapshot snapshot, Pod pod, Container container, J4pClient jolokia, ObjectName objectName) throws MalformedObjectNameException, J4pException {
        String httpUrl = getHttpUrl(pod, container, jolokia);
        String urlPrefix = snapshot.getUrlPrefix();
        if (httpUrl != null) {
            URL url = null;
            try {
                url = new URL(httpUrl);
            } catch (MalformedURLException e) {
                LOG.warn("Failed to parse http URL: " + httpUrl + ". " + e, e);
                return;
            }

            ApiDeclaration apiDeclaration = new ApiDeclaration();
            apiDeclaration.setModels(new Models());

            // lets find the rest services...
            J4pResponse<J4pExecRequest> results = jolokia.execute(new J4pExecRequest(objectName, "listRestServices"));
            Object value = results.getValue();
            if (value instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) value;
                Set<Map.Entry<String, Object>> entrySet = map.entrySet();
                for (Map.Entry<String, Object> entry : entrySet) {
                    String uriPattern = entry.getKey();
                    Object entryValue = entry.getValue();
                    if (entryValue instanceof Map) {
                        Map<String, Object> entryMap = (Map<String, Object>) entryValue;
                        Set<Map.Entry<String, Object>> operations = entryMap.entrySet();
                        for (Map.Entry<String, Object> operation : operations) {
                            String operationName = operation.getKey();
                            Object operationValue = operation.getValue();
                            if (operationValue instanceof Map) {
                                Map operationMap = (Map) operationValue;
                                CamelRestService restService = new CamelRestService();
                                try {
                                    BeanUtils.populate(restService, operationMap);
                                } catch (Exception e) {
                                    LOG.warn("Failed to populate " + restService + " from " + operationMap + ". " + e, e);
                                    return;
                                }
                                restService.setUrl(replaceHostAndPort(url, restService.getUrl()));
                                restService.setBaseUrl(replaceHostAndPort(url, restService.getBaseUrl()));

                                addToApiDescription(apiDeclaration, restService, objectName);
                            }
                        }
                    }
                }
            }

            List<Api> apis = apiDeclaration.getApis();
            if (apis != null && apis.size() > 0) {
                // TODO where should we get the API version from?

                // lets add a default version
                if (Strings.isNullOrBlank(apiDeclaration.getApiVersion())) {
                    apiDeclaration.setApiVersion("1.0");
                }
                if (apiDeclaration.getSwaggerVersion() == null) {
                    apiDeclaration.setSwaggerVersion(ApiDeclaration.SwaggerVersion._1_2);
                }
                String podId = getName(pod);
                // TODO this is not the container id...
                String containerId = getName(pod);
                PodAndContainerId key = new PodAndContainerId(podId, containerId);

                snapshot.putPodAndContainerSwagger(key, apiDeclaration);

                String basePath = apiDeclaration.getResourcePath();
                URI basePathUri = apiDeclaration.getBasePath();
                String fullUrl = null;
                if (basePathUri != null) {
                    fullUrl = basePathUri.toString();
                }

                String swaggerPath = "/swagger/pod/" + podId + "/" + containerId;
                String swaggerUrl = null;
                if (Strings.isNotBlank(urlPrefix)) {
                    swaggerUrl = urlPathJoin(urlPrefix, swaggerPath);
                }
                String wadlPath = null;
                String wadlUrl = null;
                String wsdlPath = null;
                String wsdlUrl = null;
                int port = url.getPort();

                String state = "RUNNING";
                String jolokiaUrl = jolokia.getUri().toString();
                String serviceId = null;
                ApiDTO api = new ApiDTO(pod, container, serviceId, objectName.toString(), basePath, fullUrl, port, state, jolokiaUrl, swaggerPath, swaggerUrl, wadlPath, wadlUrl, wsdlPath, wsdlUrl);
                list.add(api);
            }
        }
    }

    protected void addToApiDescription(ApiDeclaration apiDeclaration, CamelRestService restService, ObjectName objectName) {
        String basePath = restService.getBaseUrl();
        String resourcePath = restService.getBasePath();

        if (apiDeclaration.getBasePath() == null) {
            String uriText = basePath;
            if (basePath.endsWith(resourcePath)) {
                uriText = basePath.substring(0, basePath.length() - resourcePath.length());
            }

            if (replaceWithProxyLink) {
                uriText = switchToUseProxyLink(uriText);
            }
            try {
                URI uri = new URI(uriText);
                apiDeclaration.setBasePath(uri);
            } catch (URISyntaxException e) {
                LOG.warn("Could not parse basePath: " + uriText + ". " + e, e);
            }
        }
        if (Strings.isNullOrBlank(apiDeclaration.getResourcePath())) {
            apiDeclaration.setResourcePath(resourcePath);
        }


        List<Api> apis = SwaggerHelpers.getOrCreateApis(apiDeclaration);
        String path = urlPathJoin(resourcePath, restService.getUriTemplate());
        Api api = SwaggerHelpers.findOrCreateApiForPath(apis, path);
        List<Operation> operations = SwaggerHelpers.getOrCreateOperations(api);
        Operation operation = new Operation();
        String method = restService.getMethod();
        String inType = restService.getInType();
        if (Strings.isNotBlank(method)) {
            method = method.toUpperCase(Locale.US);
            operation.setMethod(Operation.Method.fromValue(method));
        }
        String description = restService.getDescription();
        if (Strings.isNotBlank(description)) {
            operation.setSummary(description);
        }
        // TODO have way to expose the nickname? Might be nice to expose the route id?
        String nickname = objectName.getKeyProperty("context");
        if (Strings.isNullOrBlank(nickname)) {
            nickname = (Strings.isNotBlank(method) ? method + " " : "") + Strings.defaultIfEmpty(inType, "") + " " + restService.getUriTemplate();
        } else {
            String route = restService.getRouteId();
            if (Strings.isNotBlank(route)) {
                nickname += "." + route;
            }
        }
        operation.setNickname(nickname);
        operation.setConsumes(splitStringToSet(restService.getConsumes(), ","));
        operation.setProduces(splitStringToSet(restService.getProduces(), ","));
        List<Parameter> parameters = SwaggerHelpers.getOrCreateParameters(operation);
        addUrlParameters(parameters, restService.getUrl());
        if (Strings.isNotBlank(inType)) {
            Parameter parameter = new Parameter();
            parameter.setParamType(Parameter.ParamType.BODY);
            parameter.setRequired(true);
            parameter.setAllowMultiple(false);
            parameter.setName("body");
            parameter.setType(inType);
            parameters.add(parameter);
        }
        String outType = restService.getOutType();
        if (Strings.isNotBlank(outType)) {
            List<ResponseMessage> responseMessages = SwaggerHelpers.getOrCreateResponseMessages(operation);
            ResponseMessage responseMessage = new ResponseMessage();
            responseMessage.setMessage(outType);
            responseMessages.add(responseMessage);
        }
        operations.add(operation);
    }

    protected String switchToUseProxyLink(String uriText) {
        try {
            URL url = new URL(uriText);
            String host = url.getHost();
            if (Strings.isNotBlank(host)) {
                int port = url.getPort();
                if (port <= 0) {
                    port = 80;
                }
                return urlPathJoin("/hawtio/proxy/" + host + "/" + port, url.getPath());
            }
        } catch (MalformedURLException e) {
            LOG.warn("Failed to parse URL " + uriText + ". " + e, e);
        }

        return uriText;
    }

    /**
     * Adds any path parameters on the URL
     */
    protected void addUrlParameters(List<Parameter> parameters, String url) {
        if (Strings.isNotBlank(url)) {
            String[] array = url.split("\\/");
            if (array != null) {
                for (String param : array) {
                    if (param.startsWith("{") && param.endsWith("}")) {
                        String name = param.substring(1, param.length() - 1);

                        Parameter parameter = new Parameter();
                        parameter.setName(name);
                        parameter.setParamType(Parameter.ParamType.PATH);
                        parameter.setRequired(true);
                        parameter.setAllowMultiple(false);
                        parameter.setType("string");
                        parameters.add(parameter);
                    }
                }
            }
        }
    }

    public static Set<String> splitStringToSet(String text, String separator) {
        Set<String> answer = new HashSet<>();
        if (Strings.isNotBlank(text)) {
            String[] split = text.split(separator);
            if (split != null) {
                for (String s : split) {
                    answer.add(s.trim());
                }
            }
        }
        return answer;
    }

    /**
     * Replaces the host and port in the given logicalUrlText with the host and port from the actualURL
     */
    protected String replaceHostAndPort(URL actualURL, String logicalUrlText) {
        if (Strings.isNotBlank(logicalUrlText)) {
            URL logicalUrl;
            try {
                logicalUrl = new URL(logicalUrlText);
            } catch (MalformedURLException e) {
                LOG.warn("Failed to parse URL " + logicalUrlText + ". " + e, e);
                return logicalUrlText;
            }
            String prefix = actualURL.toString();
            String path = logicalUrl.getPath();
            if (path == null) {
                path = "";
            }
            return urlPathJoin(prefix, path);
        }
        return logicalUrlText;
    }
}
