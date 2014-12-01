/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.gateway.http;

import io.fabric8.gateway.fabric.http.HTTPGatewayConfig;
import io.fabric8.gateway.fabric.http.HttpMappingRuleConfiguration;
import io.fabric8.gateway.loadbalancer.LoadBalancers;
import io.fabric8.utils.Systems;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.weld.environment.se.bindings.Parameters;
import org.jboss.weld.environment.se.events.ContainerInitialized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final String DEFAULT_PORT = "9090";
    public static final String DEFAULT_INDEX_ENABLED = "true";
    public static final String DEFAULT_GATEWAY_SERVICES_SELECTORS = "[{\"container\":\"java\",\"group\":\"quickstarts\"},{\"container\":\"camel\",\"group\":\"quickstarts\"}]";
    public static final String DEFAULT_URI_TEMPLATE = "/{contextPath}";
    public static final String DEFAULT_LOADBALANCER = LoadBalancers.ROUND_ROBIN_LOAD_BALANCER;
    public static final String DEFAULT_KUBERNETES_MASTER = "http://localhost:8484/";
    public static final String DEFAULT_API_MANAGER_ENABLED = "true";
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    
    @Inject HttpMappingRuleConfiguration httpMappingRuleConfigutation;
    @Inject @Parameters String[] args;

    public void main(@Observes ContainerInitialized event) {
        HTTPGatewayConfig gatewayConfig = new HTTPGatewayConfig();
        
        String serviceName = Systems.getEnvVarOrSystemProperty("HTTP_GATEWAY_SERVICE_ID", "HTTP_GATEWAY_SERVICE_ID", "FABRIC8HTTPGATEWAY").toUpperCase() + "_SERVICE_";
        String hostEnvVar = serviceName + HTTPGatewayConfig.HOST;
        String portEnvVar = HTTPGatewayConfig.HTTP_PORT;
        String apiManagerEnabledEnvVar = serviceName + HTTPGatewayConfig.IS_API_MANAGER_ENABLED;
        String kubernetesMasterEnvVar = HTTPGatewayConfig.KUBERNETES_MASTER;
        String selectorEnvVar = serviceName + HTTPGatewayConfig.SELECTORS;
        String uriTemplateEnvVar = serviceName + HTTPGatewayConfig.URI_TEMPLATE;
        String loadBalancerEnvVar = serviceName + HTTPGatewayConfig.LOAD_BALANCER;
        String enabledVersionEnvVar = serviceName + HTTPGatewayConfig.ENABLED_VERSION;
        String enableIndexEnvVar = serviceName + HTTPGatewayConfig.ENABLE_INDEX;
        String reverseHeadersEnvVar = serviceName + HTTPGatewayConfig.REVERSE_HEADERS;
        //Gateway config
        gatewayConfig.put(HTTPGatewayConfig.HOST,
                Systems.getEnvVarOrSystemProperty(hostEnvVar, hostEnvVar, DEFAULT_HOST));
        gatewayConfig.put(HTTPGatewayConfig.HTTP_PORT,
                Systems.getEnvVarOrSystemProperty(portEnvVar, portEnvVar, DEFAULT_PORT));
        gatewayConfig.put(HTTPGatewayConfig.ENABLE_INDEX,
                Systems.getEnvVarOrSystemProperty(enableIndexEnvVar, enableIndexEnvVar, DEFAULT_INDEX_ENABLED));
        gatewayConfig.put(HTTPGatewayConfig.IS_API_MANAGER_ENABLED,
        		Systems.getEnvVarOrSystemProperty(apiManagerEnabledEnvVar, apiManagerEnabledEnvVar, DEFAULT_API_MANAGER_ENABLED));
        LOG.info("Container host " + gatewayConfig.getHost());
        LOG.info("Container port " + gatewayConfig.getPort());
        LOG.info("Index enabled: " + gatewayConfig.isIndexEnabled());
        LOG.info("API Manager Enabled: " + gatewayConfig.isApiManagerEnabled());
        //Kube config
        gatewayConfig.put(HTTPGatewayConfig.KUBERNETES_MASTER,
                Systems.getEnvVarOrSystemProperty(kubernetesMasterEnvVar, kubernetesMasterEnvVar, DEFAULT_KUBERNETES_MASTER));
        LOG.info("Kubernetes Master: " + gatewayConfig.getKubernetesMaster());
        //Rule config
        gatewayConfig.put(HTTPGatewayConfig.SELECTORS,
                Systems.getEnvVarOrSystemProperty(selectorEnvVar, selectorEnvVar, DEFAULT_GATEWAY_SERVICES_SELECTORS));
        gatewayConfig.put(HTTPGatewayConfig.URI_TEMPLATE,
                Systems.getEnvVarOrSystemProperty(uriTemplateEnvVar, uriTemplateEnvVar, DEFAULT_URI_TEMPLATE));
        gatewayConfig.put(HTTPGatewayConfig.LOAD_BALANCER,
                Systems.getEnvVarOrSystemProperty(loadBalancerEnvVar, loadBalancerEnvVar, DEFAULT_LOADBALANCER));
        gatewayConfig.put(HTTPGatewayConfig.ENABLED_VERSION,
                Systems.getEnvVarOrSystemProperty(enabledVersionEnvVar, enabledVersionEnvVar, null));
        gatewayConfig.put(HTTPGatewayConfig.REVERSE_HEADERS,
                Systems.getEnvVarOrSystemProperty(reverseHeadersEnvVar, reverseHeadersEnvVar, "true"));
        LOG.info("Gateway Selector: " + gatewayConfig.get(HTTPGatewayConfig.SELECTORS));
        LOG.info("Gateway URI Template; " + gatewayConfig.get(HTTPGatewayConfig.URI_TEMPLATE));
        LOG.info("LoadBalancer: " + gatewayConfig.getLoadBalancerType());
        LOG.info("Enabled Version: " + gatewayConfig.getEnabledVersion());
        LOG.info("Reverse Headers: " + gatewayConfig.isReverseHeaders());
        LOG.info("Pushing configuration to the gateway...");
        try {
            httpMappingRuleConfigutation.configure(gatewayConfig);
            LOG.info("Gateway startup successful");
            waitUntilStop();
        } catch (Exception e) {
            LOG.error("Could not start the HTTP Gateway");
            LOG.error(e.getMessage(),e);
        }
        
    }
        
    protected static void waitUntilStop() {
        Object lock = new Object();
        while (true) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }
    
}
