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
package io.fabric8.quickstarts.swarm.route;

import org.apache.camel.builder.RouteBuilder;
import static org.apache.camel.model.rest.RestParamType.path;

public class RestService extends RouteBuilder {
    @Override public void configure() throws Exception {

        restConfiguration().component("undertow")
          // and output using pretty print
          .dataFormatProperty("prettyPrint", "true")
          // WildFly & Swarm only allows to use localhost host
          .host("localhost")
          /* DOESN'T WORK DUE TO CLASSLOADING ISSUE - https://github.com/wildfly-swarm/wildfly-swarm-camel/issues/52
          // add swagger api-doc out of the box
          .apiContextPath("/swagger.json")
          .apiProperty("api.title", "User Service")
          .apiProperty("api.version", "1.0")
          .apiProperty("api.description", "An example using REST DSL and Swagger Java with CDI")
          // and enable CORS
          .apiProperty("cors", "true")
          */
           ;

        rest("/api").description("Api rest service").consumes("application/json").produces("application/json")

           .get("/say/{name}").description("Say Hello to the name")
                .param().name("id").type(path).description("The namr of the user to say Hello").dataType("string").endParam()
                .to("direct:say");

        from("direct:say")
           .transform()
              .simple("Hello from REST endpoint to ${header.name}");
    }
}
