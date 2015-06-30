/*
 * Copyright 2005-2015 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.quickstarts.camelcdi;

import io.fabric8.annotations.Factory;
import io.fabric8.annotations.ServiceName;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;

public class ActiveMQComponentFactory {

    @Factory
    @ServiceName
    public ActiveMQComponent create(@ServiceName ActiveMQConnectionFactory factory) {
        ActiveMQComponent component = new ActiveMQComponent();
        component.setConnectionFactory(factory);
        return component;
    }

    /*
    @Factory
    @ServiceName
    public ActiveMQComponent create(@ServiceName String url, @Configuration ActiveMQConfig config) {
        ActiveMQComponent component = new ActiveMQComponent();
        component.setBrokerURL(url);
        component.setConnectionFactory(new ActiveMQConnectionFactory(url));
        return component;
    }*/

}
