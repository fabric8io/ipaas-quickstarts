/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.quickstarts.cxfcdi;

import java.util.EnumSet;
import javax.servlet.DispatcherType;

import io.fabric8.cxf.endpoint.ManagedApi;
import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;

import org.apache.cxf.cdi.CXFCdiServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.jboss.weld.environment.servlet.BeanManagerResourceBindingListener;
import org.jboss.weld.environment.servlet.Listener;

public class ApplicationStarter {

    public static void main(final String[] args) throws Exception {
        startServer().join();
    }

    public static Server startServer() throws Exception {

        // use system property first
        String port = System.getProperty("HTTP_PORT");
        if (port == null) {
            // and fallback to use environment variable
            port = System.getenv("HTTP_PORT");
        }
        if (port == null) {
            // and use port 8586 by default
            port = "8586";
        }
        Integer num = Integer.parseInt(port);
        String service = Systems.getEnvVarOrSystemProperty("WEB_CONTEXT_PATH", "WEB_CONTEXT_PATH", "");
        String servicesPath = "/servicesList";

        String servletContextPath = "/" + service;
        ManagedApi.setSingletonCxfServletContext(servletContextPath);

        System.out.println("Starting REST server at:         http://localhost:" + port + servletContextPath);
        System.out.println("View the services at:            http://localhost:" + port + servletContextPath + servicesPath);
        System.out.println("View an example REST service at: http://localhost:" + port + servletContextPath + "cxfcdi/customerservice/customers/123");
        System.out.println();

        final Server server = new Server(num);

        // Register and map the dispatcher servlet
        final ServletHolder servletHolder = new ServletHolder(new CXFCdiServlet());

        // change default service list URI
        servletHolder.setInitParameter("service-list-path", servicesPath);

        final ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addEventListener(new Listener());
        context.addEventListener(new BeanManagerResourceBindingListener());

        String servletPath = "/*";
        if (Strings.isNotBlank(service)) {
            servletPath = servletContextPath + "/*";
        }
        context.addServlet(servletHolder, servletPath);
        
        FilterHolder cors = context.addFilter(CrossOriginFilter.class,"/*",EnumSet.of(DispatcherType.REQUEST));
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "POST,GET,DELETE,OPTIONS,PUT,HEAD");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin");

        server.setHandler(context);
        
        server.start();
        return server;
    }

}
