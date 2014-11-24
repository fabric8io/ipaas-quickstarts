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

import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.CurrentState;
import io.fabric8.kubernetes.api.model.ManifestContainer;
import io.fabric8.kubernetes.api.model.PodSchema;
import io.fabric8.kubernetes.api.model.Port;
import io.fabric8.kubernetes.api.model.ServiceSchema;
import io.fabric8.kubernetes.jolokia.JolokiaClients;
import io.fabric8.utils.Closeables;
import io.fabric8.utils.Strings;
import io.fabric8.utils.URLUtils;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pResponse;
import org.jolokia.client.request.J4pSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.fabric8.utils.URLUtils.urlPathJoin;

/**
 */
public class ApiFinder {
    private static final transient Logger LOG = LoggerFactory.getLogger(ApiFinder.class);

    public static final String CXF_API_ENDPOINT_MBEAN_NAME = "io.fabric8.cxf:*";
    public static final String SWAGGER_POSTFIX = "/api-docs";
    public static final String WADL_POSTFIX = "?_wadl";
    public static final String WSDL_POSTFIX = "?wsdl";

    JolokiaClients clients = new JolokiaClients();
    Kubernetes kubernetes = clients.getKubernetes();


    public static final String swaggerProperty = "Swagger";
    public static final String wadlProperty = "WADL";
    public static final String wsdlProperty = "WSDL";


    public List<ApiDTO> findServicesOnPods(String selector) {
        List<ApiDTO> answer = new ArrayList<>();
        Map<String, PodSchema> podMap = KubernetesHelper.getPodMap(kubernetes, selector);
        Collection<PodSchema> pods = podMap.values();
        for (PodSchema pod : pods) {
            String host = KubernetesHelper.getHost(pod);
            List<ManifestContainer> containers = KubernetesHelper.getContainers(pod);
            for (ManifestContainer container : containers) {
                J4pClient jolokia = clients.jolokiaClient(host, container, pod);
                if (jolokia != null) {
                    List<ApiDTO> apiDTOs = findServices(pod, container, jolokia);
                    answer.addAll(apiDTOs);
                }
            }
        }
        Map<String, ServiceSchema> serviceMap = KubernetesHelper.getServiceMap(kubernetes);
        addDiscoveredServiceApis(answer, serviceMap);
        return answer;
    }

    protected void addDiscoveredServiceApis(List<ApiDTO> apis, Map<String, ServiceSchema> serviceMap) {
        CloseableHttpClient client = createHttpClient();
        try {
            Set<Map.Entry<String, ServiceSchema>> entries = serviceMap.entrySet();
            for (Map.Entry<String, ServiceSchema> entry : entries) {
                String key = entry.getKey();
                ServiceSchema service = entry.getValue();
                String id = service.getId();
                String url = KubernetesHelper.getServiceURL(service);
                if (Strings.isNotBlank(url)) {
                    // lets check if we've not got this service already
                    if (!apiExistsForUrl(apis, url)) {
                        tryFindApis(client, apis, service, url);
                    }
                }
            }
        } finally {
            Closeables.closeQuietly(client);
        }
    }

    protected CloseableHttpClient createHttpClient() {
        return HttpClientBuilder.create().build();
    }

    protected void tryFindApis(CloseableHttpClient client, List<ApiDTO> apis, ServiceSchema service, String url) {
        String podId = null;
        String containerName = null;
        String objectName = null;
        String serviceId = service.getId();
        Map<String, String> labels = service.getLabels();
        String state = "STARTED";

        int port = 0;
        String jolokiaUrl = null;
        String swaggerUrl = urlPathJoin(url, SWAGGER_POSTFIX);
        String wadlUrl = null;
        String wsdlUrl = null;

        String path = toPath(url);
        String swaggerPath = toPath(swaggerUrl);
        String wadlPath = toPath(wadlUrl);
        String wsdlPath = toPath(wsdlUrl);

        boolean valid = isValidApiEndpoint(client, swaggerUrl);
        if (valid) {
            apis.add(new ApiDTO(podId, serviceId, labels, containerName, objectName, path, url, port, state, jolokiaUrl, swaggerPath, swaggerUrl, wadlPath, wadlUrl, wsdlPath, wsdlUrl));
        }
    }

    /**
     * Returns the path for the given URL
     */
    public static String toPath(String url) {
        if (Strings.isNullOrBlank(url)) {
            return null;
        }
        int idx = url.indexOf("://");
        if (idx <= 0) {
            idx = url.indexOf(":");
            if (idx >= 0) {
                idx++;
            } else {
                idx = 0;
            }
        } else {
            idx += 3;
        }
        idx = url.indexOf("/", idx);
        return url.substring(idx);
    }

    protected boolean isValidApiEndpoint(CloseableHttpClient client, String url) {
        boolean valid = false;
        try {
            CloseableHttpResponse response = client.execute(new HttpGet(url));
            StatusLine statusLine = response.getStatusLine();
            if (statusLine != null) {
                int statusCode = statusLine.getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    valid = true;
                }
            }
        } catch (IOException e) {
            LOG.debug("Ignored failed request on " + url + ". " + e, e);
        }
        return valid;
    }

    protected boolean apiExistsForUrl(List<ApiDTO> apis, String url) {
        for (ApiDTO api : apis) {
            String apiUrl = api.getUrl();
            if (apiUrl != null && apiUrl.startsWith(url)) {
                return true;
            }
        }
        return false;
    }

    protected List<ApiDTO> findServices(PodSchema pod, ManifestContainer container, J4pClient jolokia) {
        List<ApiDTO> answer = new ArrayList<>();
        try {
            J4pResponse<J4pSearchRequest> results = jolokia.execute(new J4pSearchRequest(CXF_API_ENDPOINT_MBEAN_NAME));
            Object value = results.getValue();
            if (value instanceof List) {
                List<String> list = (List<String>) value;
                for (String objectName : list) {
                    if (Strings.isNotBlank(objectName)) {
                        ApiDTO apiDTO = findCxfApi(pod, container, jolokia, objectName);
                        if (apiDTO != null) {
                            answer.add(apiDTO);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to look up attribute. " + e, e);
        }
        return answer;
    }

    protected ApiDTO findCxfApi(PodSchema pod, ManifestContainer container, J4pClient jolokia, String mbean) throws MalformedObjectNameException, J4pException {
        ObjectName objectName = new ObjectName(mbean);
        String type = objectName.getKeyProperty("type");
        if (type != null && "Bus.Service.Endpoint".equals(type)) {
            J4pResponse<J4pReadRequest> results = jolokia.execute(new J4pReadRequest(objectName,
                    "State", "Address", "ServletContext", swaggerProperty, wadlProperty, wsdlProperty));
            Object value = results.getValue();
            if (value instanceof Map) {
                Map map = (Map) value;
                Object stateValue = map.get("State");
                Object addressValue = map.get("Address");
                Object servletContextValue = map.get("ServletContext");
                if (stateValue instanceof String && addressValue instanceof String && servletContextValue instanceof String) {
                    String state = (String) stateValue;
                    String address = (String) addressValue;
                    String servletContext = (String) servletContextValue;
                    if (Strings.isNotBlank(state) && Strings.isNotBlank(address) && Strings.isNotBlank(servletContext)) {
                        String stateUpper = state.toUpperCase();
                        boolean started = stateUpper.startsWith("START");
                        boolean created = stateUpper.startsWith("CREATE");
                        if ((started || created)) {
                            String httpUrl = getHttpUrl(pod, container, jolokia);
                            if (httpUrl != null) {
                                String basePath = urlPathJoin(servletContext, address);
                                String fullUrl = urlPathJoin(httpUrl, basePath);

                                String swaggerPath = null;
                                String swaggerUrl = null;
                                String wadlPath = null;
                                String wadlUrl = null;
                                String wsdlPath = null;
                                String wsdlUrl = null;
                                int port = 80;
                                try {
                                    URL url = new URL(httpUrl);
                                    port = url.getPort();
                                } catch (Exception e) {
                                    // ignore
                                }

                                if (booleanAttribute(map, swaggerProperty)) {
                                    swaggerPath = urlPathJoin(basePath, SWAGGER_POSTFIX);
                                    swaggerUrl = urlPathJoin(httpUrl, swaggerPath);
                                }
                                if (booleanAttribute(map, wadlProperty)) {
                                    wadlPath = basePath + WADL_POSTFIX;
                                    wadlUrl = urlPathJoin(httpUrl, wadlPath);
                                }
                                if (booleanAttribute(map, wsdlProperty)) {
                                    wsdlPath = basePath + WSDL_POSTFIX;
                                    wsdlUrl = urlPathJoin(httpUrl, wsdlPath);
                                }
                                String jolokiaUrl = jolokia.getUri().toString();
                                String serviceId = null;
                                return new ApiDTO(pod, container, serviceId, objectName.toString(), basePath, fullUrl, port, state, jolokiaUrl, swaggerPath, swaggerUrl, wadlPath, wadlUrl, wsdlPath, wsdlUrl);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }


    protected String getHttpUrl(PodSchema pod, ManifestContainer container, J4pClient jolokia) {
        // lets find the HTTP port
        if (container != null) {
            List<Port> ports = container.getPorts();
            for (Port port : ports) {
                Integer containerPort = port.getContainerPort();
                if (containerPort != null) {
                    String name = port.getName();
                    if (name != null) {
                        name = name.toLowerCase();
                    }

                    boolean httpPortNumber = containerPort == 80 || containerPort == 443 || containerPort == 8080 || containerPort == 8181;
                    boolean httpName = Objects.equals("http", name) || Objects.equals("https", name);
                    String protocolName = containerPort == 443 || Objects.equals("https", name) ? "https" : "http";

                    if (httpPortNumber || (httpName && containerPort.intValue() > 0)) {
                        CurrentState currentState = pod.getCurrentState();
                        if (currentState != null) {
                            String podIP = currentState.getPodIP();
                            if (Strings.isNotBlank(podIP)) {
                                return protocolName + "://" + podIP + ":" + containerPort;
                            }

                            // lets try use the host port and host name on jube
                            String host = currentState.getHost();
                            Integer hostPort = port.getHostPort();
                            if (Strings.isNotBlank(host) && hostPort != null) {
                                return protocolName + "://" + host + ":" + hostPort;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    protected static boolean booleanAttribute(Map map, String propertyName) {
        Object value = map.get(propertyName);
        if (value instanceof Boolean) {
            Boolean b = (Boolean) value;
            return b.booleanValue();
        } else {
            return false;
        }
    }
}
