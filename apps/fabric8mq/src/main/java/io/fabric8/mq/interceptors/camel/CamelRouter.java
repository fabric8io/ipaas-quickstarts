/*
 *
 *  * Copyright 2005-2015 Red Hat, Inc.
 *  * Red Hat licenses this file to you under the Apache License, version
 *  * 2.0 (the "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  * implied.  See the License for the specific language governing
 *  * permissions and limitations under the License.
 *
 */

package io.fabric8.mq.interceptors.camel;

import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CamelRouter extends ServiceSupport {
    public static final String REGISTRY_NAME = "camel";
    private static Logger LOG = LoggerFactory.getLogger(CamelRouter.class);
    private String camelRoutes;
    private DefaultCamelContext camelContext;

    public String getCamelRoutes() {
        return camelRoutes;
    }

    public void setCamelRoutes(String camelRoutes) {
        this.camelRoutes = camelRoutes;
        if (isStarted()) {
            doLoadRoutes();
        }
    }

    protected void doStart() throws Exception {
        String routesString = getCamelRoutes();
        if (routesString != null) {
            routesString = routesString.trim();
        }
        if (routesString != null && !routesString.isEmpty()) {
            LOG.info("Starting Camel Router");
            camelContext = new DefaultCamelContext();
            camelContext.setName("Fabric8MQ-Camel-Router");
            camelContext.start();
            doLoadRoutes();
        } else {
            LOG.info("No camel routes to getLoad");
        }
    }

    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        if (camelContext != null) {
            camelContext.stop();
        }
    }

    private void doLoadRoutes() {
        String routesString = getCamelRoutes();
        routesString = routesString.trim();
        if (!routesString.matches("route>")) {
            String lines[] = routesString.split("\\r?\\n");
            routesString = "";
            for (int i= 0; i < lines.length;i++){
                routesString +=  "<route>" + lines[i]+  "</route>" +  System.lineSeparator();
            }
        }
        if (!routesString.matches("<routes")) {
            String str = "<routes xmlns=\"http://camel.apache.org/schema/spring\">";
            str += System.lineSeparator();
            str += routesString;
            str += System.lineSeparator();
            str += "</routes>";
            routesString = str;
        }
        LOG.info("Loading Camel Routes " + routesString);

        try {
            InputStream is = new ByteArrayInputStream(routesString.getBytes(StandardCharsets.UTF_8));

            List<RouteDefinition> currentRoutes = camelContext.getRouteDefinitions();
            for (RouteDefinition rd : currentRoutes) {
                camelContext.stopRoute(rd);
                camelContext.removeRouteDefinition(rd);
            }

            RoutesDefinition routesDefinition = camelContext.loadRoutesDefinition(is);
            camelContext.addRouteDefinitions(routesDefinition.getRoutes());
            is.close();
        } catch (Throwable e) {
            LOG.error("Failed to getLoad routes " + e.getMessage(), e);
        }
    }

}
