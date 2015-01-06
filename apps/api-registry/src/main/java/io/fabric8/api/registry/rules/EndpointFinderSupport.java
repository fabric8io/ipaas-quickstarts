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
import org.jolokia.client.request.J4pResponse;
import org.jolokia.client.request.J4pSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.List;

/**
 */
public abstract class EndpointFinderSupport implements io.fabric8.api.registry.EndpointFinder {
    private static final transient Logger LOG = LoggerFactory.getLogger(EndpointFinderSupport.class);

    @Override
    public List<ApiDTO> findApis(ApiSnapshot snapshot, Pod pod, Container container, J4pClient jolokia) {
        List<ApiDTO> answer = new ArrayList<>();
        try {
            J4pResponse<J4pSearchRequest> results = jolokia.execute(new J4pSearchRequest(getObjectNamePattern()));
            Object value = results.getValue();
            if (value instanceof List) {
                List<String> list = (List<String>) value;
                for (String mbeanName : list) {
                    if (Strings.isNotBlank(mbeanName)) {
                        ObjectName objectName = new ObjectName(mbeanName);
                        appendObjectNameEndpoints(answer, snapshot, pod, container, jolokia, objectName);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to look up attribute. " + e, e);
        }
        return answer;
    }

    protected abstract String getObjectNamePattern();

    protected abstract void appendObjectNameEndpoints(List<ApiDTO> list, ApiSnapshot snapshot, Pod pod, Container container, J4pClient jolokia, ObjectName objectName) throws MalformedObjectNameException, J4pException;
}
