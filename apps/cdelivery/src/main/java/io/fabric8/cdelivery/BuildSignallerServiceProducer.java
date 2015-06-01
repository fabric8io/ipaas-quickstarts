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
package io.fabric8.cdelivery;

import io.fabric8.io.fabric8.workflow.build.signal.BuildSignallerService;

import javax.enterprise.inject.Produces;

import io.fabric8.annotations.Protocol;
import io.fabric8.annotations.ServiceName;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.kie.api.runtime.KieSession;

/**
 */
public class BuildSignallerServiceProducer {

    @Produces
    public BuildSignallerService createBuildSignallerService(@Protocol("http") @ServiceName("fabric8") String consoleLink,
                                                             @ConfigProperty(name = "BUILD_NAMESPACE", defaultValue = "") String namespace,
                                                             KieSession ksession) {
        BuildSignallerService service = new BuildSignallerService(ksession, namespace);
        service.setConsoleLink(consoleLink);
        service.start();
        return service;
    }
}
