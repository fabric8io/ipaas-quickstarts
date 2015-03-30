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
package io.fabric8.hubot.notifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.cfg.Annotations;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import io.fabric8.annotations.Protocol;
import io.fabric8.annotations.ServiceName;
import io.fabric8.kubernetes.api.ExceptionResponseMapper;
import io.fabric8.utils.Strings;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static io.fabric8.kubernetes.api.KubernetesFactory.createObjectMapper;

/**
 * A service for notifying a message to hubot
 */
public class HubotNotifier {
    private static final transient Logger LOG = LoggerFactory.getLogger(HubotNotifier.class);

    private final String hubotUrl;
    private final String username;
    private final String password;
    private HubotRestApi api;

    @Inject
    public HubotNotifier(@Protocol("http") @ServiceName("hubot-web-hook") String hubotUrl,
                         @ConfigProperty(name = "HUBOT_USERNAME", defaultValue = "") String username,
                         @ConfigProperty(name = "HUBOT_PASSWORD", defaultValue = "") String password) {
        this.hubotUrl = hubotUrl;
        this.username = username;
        this.password = password;
        LOG.info("Starting HubotNotifier using address: " + hubotUrl);
    }

    public void notify(String room, String message) {
        LOG.info("About to notify room: " + room + " message: " + message);
        try {
            getHubotRestApi().notify(room, message);
        } catch (Exception e) {
            LOG.error("Failed to notify hubot room: " + room + " with message: " + message + ". Reason: " + e, e);
        }
    }


    protected HubotRestApi getHubotRestApi() {
        if (api == null) {
            api = createWebClient(HubotRestApi.class);
        }
        return api;
    }

    /**
     * Creates a JAXRS web client for the given JAXRS client
     */
    protected <T> T createWebClient(Class<T> clientType) {
        List<Object> providers = createProviders();
        WebClient webClient = WebClient.create(hubotUrl, providers);
        if (Strings.isNotBlank(username) && Strings.isNotBlank(password)) {
            HTTPConduit conduit = WebClient.getConfig(webClient).getHttpConduit();
            conduit.getAuthorization().setUserName(username);
            conduit.getAuthorization().setPassword(password);
        }
        return JAXRSClientFactory.fromClient(webClient, clientType);
    }

    protected List<Object> createProviders() {
        List<Object> providers = new ArrayList<Object>();
        Annotations[] annotationsToUse = JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS;
        ObjectMapper objectMapper = createObjectMapper();
        providers.add(new JacksonJaxbJsonProvider(objectMapper, annotationsToUse));
        providers.add(new ExceptionResponseMapper());
        return providers;
    }

}
