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
package io.fabric8.quickstarts.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.metrics.routepolicy.MetricsRoutePolicyFactory;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamelConfig extends CamelConfiguration {

    @Bean
    public RouteBuilder route() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("timer://foo?period=5000&daemon=false").
                        setBody().constant("<hello>world!</hello>").
                        log(">>> ${body}");
            }
        };
    }

    @Override
    protected void setupCamelContext(CamelContext camelContext) throws Exception {
        // make Camel aware of Spring Boot’s application.properties
        PropertiesComponent pc = new PropertiesComponent();
        pc.setLocation("classpath:application.properties");
        camelContext.addComponent("properties", pc);

        // enable performance metrics
        camelContext.addRoutePolicyFactory(new MetricsRoutePolicyFactory());

        super.setupCamelContext(camelContext);
    }
}
