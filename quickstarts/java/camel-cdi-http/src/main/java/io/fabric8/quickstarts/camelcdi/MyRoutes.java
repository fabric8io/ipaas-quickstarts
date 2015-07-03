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
package io.fabric8.quickstarts.camelcdi;

import javax.inject.Inject;

import org.apache.camel.Endpoint;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.cdi.Uri;

/**
 * Configures all our Camel routes, components, endpoints and beans
 */
@ContextName("myCdiCamelContext")
public class MyRoutes extends RouteBuilder {

    @Inject @Uri("timer:foo?period=5000")
    private Endpoint inputEndpoint;

    // TODO: use quickstart-camelservlet when upgrading to Camel 2.15.3+
    @Inject @Uri("netty4-http:http://{{service:QUICKSTART_CAMELSERVLET}}/camel/hello?keepAlive=false&disconnect=true")
    private Endpoint httpEndpoint;

    @Inject @Uri("log:output?showExchangePattern=false&showStreams=true")
    private Endpoint resultEndpoint;

    @Inject
    private SomeBean someBean;

    @Override
    public void configure() throws Exception {
        // you can configure the route rule with Java DSL here

        from(inputEndpoint)
            .setHeader("name", method(someBean))
            .to(httpEndpoint)
            .to(resultEndpoint);
    }

}
