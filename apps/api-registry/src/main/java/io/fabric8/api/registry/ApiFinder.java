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
import io.fabric8.kubernetes.api.model.ReplicationControllerSchema;
import io.fabric8.kubernetes.jolokia.JolokiaClients;
import io.fabric8.utils.Strings;
import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pResponse;
import org.jolokia.client.request.J4pSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 */
public class ApiFinder {
    private static final transient Logger LOG = LoggerFactory.getLogger(ApiFinder.class);

    public static final String CXF_API_ENDPOINT_MBEAN_NAME = "io.fabric8.cxf:*";

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
        return answer;
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
                                String basePath = servletContext + address;
                                String fullUrl = httpUrl + basePath;

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
                                    swaggerPath = basePath + "/api-docs";
                                    swaggerUrl = httpUrl + swaggerPath;
                                }
                                if (booleanAttribute(map, wadlProperty)) {
                                    wadlPath = basePath + "?_wadl";
                                    wadlUrl = httpUrl + wadlPath;
                                }
                                if (booleanAttribute(map, wsdlProperty)) {
                                    wsdlPath = basePath + "?wsdl";
                                    wsdlUrl = httpUrl + wsdlPath;
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
