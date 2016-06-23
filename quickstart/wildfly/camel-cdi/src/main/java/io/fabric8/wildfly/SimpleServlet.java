/*
 * #%L
 * Wildfly Camel :: Example :: Camel CDI
 * %%
 * Copyright (C) 2013 - 2014 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.fabric8.wildfly;

import java.io.IOException;
import java.io.PrintStream;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;

@SuppressWarnings("serial")
@WebServlet(name = "HttpServiceServlet", urlPatterns = { "/*" }, loadOnStartup = 1)
public class SimpleServlet extends HttpServlet
{
	@Inject
	private CamelContext camelctx;

    @Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        
        String name = req.getParameter("name");
        name = name != null ? name : "NoName";
        
        res.setContentType("text/html;charset=UTF-8");
    	try (ServletOutputStream out = res.getOutputStream()) {
    	    
            String result;
            try {
                ProducerTemplate producer = camelctx.createProducerTemplate();
                result = producer.requestBody("direct:start", name, String.class);
            } catch (Exception ex) {
                ex.printStackTrace(new PrintStream(out));
                throw new ServletException(ex);
            }
            
            String env = System.getenv().get("HOSTNAME");

            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>WildFly on Kubernetes/OpenShift with Fabric8</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>WildFly on Kubernetes/OpenShift with Fabric8 at " + req.getServerName() + "</h1>");
            out.println("<b>Pod name: " + env + "<b/><p/>");
            out.println(result + "!");
            out.println("</body>");
            out.println("</html>");
        }
    }
}
