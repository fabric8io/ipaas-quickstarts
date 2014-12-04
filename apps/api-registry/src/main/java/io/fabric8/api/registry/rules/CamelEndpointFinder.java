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
import io.fabric8.kubernetes.api.model.ManifestContainer;
import io.fabric8.kubernetes.api.model.PodSchema;
import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pException;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.net.URL;
import java.util.List;

import static io.fabric8.api.registry.ApiFinder.getHttpUrl;
import static io.fabric8.utils.URLUtils.urlPathJoin;

/**
 * Rule to find <a href="http://camel.apache.org/">Apache Camel</a> REST endpoints via JMX
 */
public class CamelEndpointFinder extends EndpointFinderSupport {

    public static final String CXF_API_ENDPOINT_MBEAN_NAME = "org.apache.camel:*";

    @Override
    protected String getObjectNamePattern() {
        return CXF_API_ENDPOINT_MBEAN_NAME;
    }

    @Override
    protected void appendObjectNameEndpoints(List<ApiDTO> list, PodSchema pod, ManifestContainer container, J4pClient jolokia, ObjectName objectName) throws MalformedObjectNameException, J4pException {
        String httpUrl = getHttpUrl(pod, container, jolokia);
        if (httpUrl != null) {
            // TODO fixme...
            String servletContext = "";
            String address = "";

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

            String state = "RUNNING";
            String jolokiaUrl = jolokia.getUri().toString();
            String serviceId = null;
            ApiDTO api = new ApiDTO(pod, container, serviceId, objectName.toString(), basePath, fullUrl, port, state, jolokiaUrl, swaggerPath, swaggerUrl, wadlPath, wadlUrl, wsdlPath, wsdlUrl);
/*
            TODO

            list.add(api);
*/
        }
    }
}
