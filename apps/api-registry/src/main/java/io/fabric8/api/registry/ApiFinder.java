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

import io.fabric8.api.registry.rules.CamelEndpointFinder;
import io.fabric8.api.registry.rules.CxfEndpointFinder;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.jolokia.JolokiaClients;
import io.fabric8.swagger.model.ApiDeclaration;
import io.fabric8.utils.Closeables;
import io.fabric8.utils.Filter;
import io.fabric8.utils.Strings;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jolokia.client.J4pClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import static io.fabric8.kubernetes.api.KubernetesHelper.getName;
import static io.fabric8.utils.URLUtils.urlPathJoin;

/**
 */
public class ApiFinder {
    private static final transient Logger LOG = LoggerFactory.getLogger(ApiFinder.class);

    public static final String SWAGGER_POSTFIX = "/api-docs";
    public static final String WADL_POSTFIX = "?_wadl";
    public static final String WSDL_POSTFIX = "?wsdl";

    public static final String swaggerProperty = "Swagger";
    public static final String wadlProperty = "WADL";
    public static final String wsdlProperty = "WSDL";

    private final long pollTimeMs;
    private MessageContext messageContext;
    private JolokiaClients clients = new JolokiaClients();
    private KubernetesClient kubernetes = clients.getKubernetes();
    private AtomicReference<ApiSnapshot> snapshotCache = new AtomicReference<>();
    private Timer timer = new Timer();
    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
            refreshSnapshot();
        }
    };
    private List<EndpointFinder> finders = new ArrayList<>();
    private String urlPrefix;

    /**
     * @param pollTimeMs the polling time interval in milliseconds
     */
    @Inject
    public ApiFinder(@ConfigProperty(name = "API_REGISTRY_POLL_TIME", defaultValue = "5000") long pollTimeMs) {
        this.pollTimeMs = pollTimeMs;
        timer.schedule(task, 0, pollTimeMs);
        finders.addAll(createDefaultEndpointFinders());
    }

    @PreDestroy
    public void destroy() {
        timer.cancel();
    }


    public ApiSnapshot refreshSnapshot() {
        Map<String, Pod> podMap = KubernetesHelper.getPodMap(kubernetes);
        ApiSnapshot snapshot = new ApiSnapshot(podMap);
        snapshot.setUrlPrefix(urlPrefix);
        snapshot.setMessageContext(messageContext);
        pollPodApis(snapshot);
        pollServiceApis(snapshot);
        snapshotCache.set(snapshot);
        return snapshot;
    }

    protected void pollPodApis(ApiSnapshot snapshot) {
        Map<String, List<ApiDTO>> answer = new HashMap<>();
        Set<Map.Entry<String, Pod>> entries = snapshot.getPodMap().entrySet();
        for (Map.Entry<String, Pod> entry : entries) {
            String podId = entry.getKey();
            Pod pod = entry.getValue();
            List<ApiDTO> list = new ArrayList<ApiDTO>();
            addApisForPod(snapshot, pod, list);
            if (list != null && !list.isEmpty()) {
                snapshot.putApis(podId, list);
            }
        }
    }

    public List<ApiDTO> findApisOnPods(String selector) {
        List<ApiDTO> answer = new ArrayList<>();
        Filter<Pod> filter = KubernetesHelper.createPodFilter(selector);
        ApiSnapshot snapshot = getSnapshot();
        snapshot.setMessageContext(messageContext);

        Map<String, Pod> podMap = snapshot.getPodMap();
        Map<String, List<ApiDTO>> cache = snapshot.getApiMap();

        Set<Map.Entry<String, Pod>> entries = podMap.entrySet();
        for (Map.Entry<String, Pod> entry : entries) {
            String key = entry.getKey();
            Pod pod = entry.getValue();
            if (filter.matches(pod)) {
                List<ApiDTO> dtos = cache.get(key);
                if (dtos != null) {
                    answer.addAll(dtos);
                }
            }
        }
        completeMissingFullUrls(answer);
        return answer;
    }

    /**
     * When we created the APIs we may not have had the {@link #urlPrefix} so we may not have completed the full URLs
     * for things like swagger. So lets fix them up now
     *
     * @param apis
     */
    protected void completeMissingFullUrls(List<ApiDTO> apis) {
        for (ApiDTO api : apis) {
            String swaggerUrl = api.getSwaggerUrl();
            String swaggerPath = api.getSwaggerPath();
            if (Strings.isNotBlank(swaggerPath) && Strings.isNullOrBlank(swaggerUrl) && Strings.isNotBlank(urlPrefix)) {
                swaggerUrl = urlPathJoin(urlPrefix, swaggerPath);
                api.setSwaggerUrl(swaggerUrl);
            }
        }
    }

    public ApiSnapshot getSnapshot() {
        ApiSnapshot snapshot = snapshotCache.get();
        if (snapshot == null) {
            snapshot = refreshSnapshot();
        }
        return snapshot;
    }

    public void addApisForPod(ApiSnapshot snapshot, Pod pod, List<ApiDTO> list) {
        String host = KubernetesHelper.getHost(pod);
        List<Container> containers = KubernetesHelper.getContainers(pod);
        for (Container container : containers) {
            J4pClient jolokia = clients.clientForContainer(host, container, pod);
            if (jolokia != null) {
                List<ApiDTO> apiDTOs = findServices(snapshot, pod, container, jolokia);
                list.addAll(apiDTOs);
            }
        }
    }

    public List<ApiDTO> findApisOnServices(String selector) {
        ApiSnapshot snapshot = getSnapshot();
        snapshot.setMessageContext(messageContext);
        List<ApiDTO> answer = snapshot.getServiceApis();
        completeMissingFullUrls(answer);
        return answer;
    }

    protected void pollServiceApis(ApiSnapshot snapshot) {
        List<ApiDTO> answer = new ArrayList<>();
        Map<String, Service> serviceMap = KubernetesHelper.getServiceMap(kubernetes);

        // TODO pick a pod for each service and add its APIs?
        addDiscoveredServiceApis(snapshot, serviceMap, messageContext);
    }

    protected void addDiscoveredServiceApis(ApiSnapshot snapshot, Map<String, Service> serviceMap, MessageContext messageContext) {
        CloseableHttpClient client = createHttpClient();
        try {
            Set<Map.Entry<String, Service>> entries = serviceMap.entrySet();
            for (Map.Entry<String, Service> entry : entries) {
                String key = entry.getKey();
                Service service = entry.getValue();
                String id = getName(service);
                String url = KubernetesHelper.getServiceURL(service);
                if (Strings.isNotBlank(url)) {
                    // lets check if we've not got this service already
                    if (!apiExistsForUrl(snapshot.getServiceApis(), url)) {
                        tryFindApis(client, snapshot, service, url, messageContext);
                    }
                }
            }
        } finally {
            Closeables.closeQuietly(client);
        }
    }


    /**
     * Returns the default endpoint finder rules
     */
    protected List<? extends EndpointFinder> createDefaultEndpointFinders() {
        return Arrays.asList(new CxfEndpointFinder(), new CamelEndpointFinder());
    }

    protected CloseableHttpClient createHttpClient() {
        return HttpClientBuilder.create().build();
    }

    protected void tryFindApis(CloseableHttpClient client, ApiSnapshot snapshot, Service service, String url, MessageContext messageContext) {
        String podId = null;
        String containerName = null;
        String objectName = null;
        String serviceId = getName(service);
        Map<String, String> labels = KubernetesHelper.getLabels(service.getMetadata());
        String state = "STARTED";

        int port = 0;
        String jolokiaUrl = null;
        String wadlUrl = null;
        String wsdlUrl = null;

        String path = toPath(url);
        String wadlPath = toPath(wadlUrl);
        String wsdlPath = toPath(wsdlUrl);

        List<String> swaggerPostfixes = new ArrayList<>();

        // TODO use env vars to figure out what postfix to use for what service id?
        swaggerPostfixes.add(SWAGGER_POSTFIX);
        swaggerPostfixes.add("swaggerapi");

        for (String swaggerPostfix : swaggerPostfixes) {
            String swaggerUrl = urlPathJoin(url, swaggerPostfix);
            String swaggerPath = toPath(swaggerUrl);
            try {
                boolean valid = isValidApiEndpoint(client, swaggerUrl);
                if (valid) {
                    snapshot.addServiceApi(new ApiDTO(podId, serviceId, labels, containerName, objectName, path, url, port, state, jolokiaUrl, swaggerPath, swaggerUrl, wadlPath, wadlUrl, wsdlPath, wsdlUrl));
                    break;
                }
            } catch (Throwable e) {
                LOG.error("Failed to discover any APIs for " + url + ". " + e, e);
            }
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
        if (idx < 0) {
            return null;
        }
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

    protected List<ApiDTO> findServices(ApiSnapshot snapshot, Pod pod, Container container, J4pClient jolokia) {
        List<ApiDTO> answer = new ArrayList<>();
        for (EndpointFinder finder : finders) {
            List<ApiDTO> apis = finder.findApis(snapshot, pod, container, jolokia);
            if (apis != null) {
                answer.addAll(apis);
            }
        }
        return answer;
    }


    public static String getHttpUrl(Pod pod, Container container, J4pClient jolokia) {
        // lets find the HTTP port
        if (container != null) {
            List<ContainerPort> ports = container.getPorts();
            for (ContainerPort port : ports) {
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
                        PodStatus currentState = pod.getStatus();
                        if (currentState != null) {
                            String podIP = currentState.getPodIP();
                            if (Strings.isNotBlank(podIP)) {
                                return protocolName + "://" + podIP + ":" + containerPort;
                            }

                            // lets try use the host port and host name on jube
                            String host = currentState.getHostIP();
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

    public static boolean booleanAttribute(Map map, String propertyName) {
        Object value = map.get(propertyName);
        if (value instanceof Boolean) {
            Boolean b = (Boolean) value;
            return b.booleanValue();
        } else {
            return false;
        }
    }

    public ApiDeclaration getSwaggerForPodAndContainer(PodAndContainerId key) {
        return getSnapshot().getSwaggerForPodAndContainer(key);
    }

    public MessageContext getMessageContext() {
        return messageContext;
    }

    public void setMessageContext(MessageContext messageContext) {
        this.messageContext = messageContext;
    }

    public void setUrlPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;

    }

    public String getUrlPrefix() {
        return urlPrefix;
    }
}
