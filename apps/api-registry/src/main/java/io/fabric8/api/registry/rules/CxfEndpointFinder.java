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
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.utils.Strings;
import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pResponse;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static io.fabric8.api.registry.ApiFinder.SWAGGER_POSTFIX;
import static io.fabric8.api.registry.ApiFinder.WADL_POSTFIX;
import static io.fabric8.api.registry.ApiFinder.WSDL_POSTFIX;
import static io.fabric8.api.registry.ApiFinder.booleanAttribute;
import static io.fabric8.api.registry.ApiFinder.getHttpUrl;
import static io.fabric8.api.registry.ApiFinder.swaggerProperty;
import static io.fabric8.api.registry.ApiFinder.wadlProperty;
import static io.fabric8.api.registry.ApiFinder.wsdlProperty;
import static io.fabric8.utils.URLUtils.urlPathJoin;

/**
 * Rule to find <a href="http://cxf.apache.org/">Apache CXF</a> endpoints via JMX
 */
public class CxfEndpointFinder extends EndpointFinderSupport {

    public static final String CXF_API_ENDPOINT_MBEAN_NAME = "io.fabric8.cxf:*";

    @Override
    protected String getObjectNamePattern() {
        return CXF_API_ENDPOINT_MBEAN_NAME;
    }

    @Override
    protected void appendObjectNameEndpoints(List<ApiDTO> list, ApiSnapshot snapshot, Pod pod, Container container, J4pClient jolokia, ObjectName objectName) throws MalformedObjectNameException, J4pException {
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
                                ApiDTO api = new ApiDTO(pod, container, serviceId, objectName.toString(), basePath, fullUrl, port, state, jolokiaUrl, swaggerPath, swaggerUrl, wadlPath, wadlUrl, wsdlPath, wsdlUrl);
                                list.add(api);
                            }
                        }
                    }
                }
            }
        }
    }
}
