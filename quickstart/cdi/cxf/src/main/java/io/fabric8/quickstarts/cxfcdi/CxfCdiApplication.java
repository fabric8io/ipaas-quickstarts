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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import io.fabric8.cxf.endpoint.EnableJMXFeature;
import io.fabric8.utils.Manifests;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxrs.swagger.Swagger2Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationPath("cxfcdi")
public class CxfCdiApplication extends Application {
    @Inject
    private CustomerService customerService;
    @Inject
    private RootService rootService;
    @Produces
    private JacksonJsonProvider jacksonJsonProvider = new JacksonJsonProvider();

    private static final Logger LOG = LoggerFactory.getLogger(CustomerService.class);
    private static Swagger2Feature swaggerFeature;

    @Override
    public Set<Object> getSingletons() {

        if (swaggerFeature == null) {
            swaggerFeature = new Swagger2Feature();
            try {
                Manifest manifest = Manifests.getManifestFromCurrentJar(getClass());
                Map<Manifests.Attribute, String> projectInfo = Manifests.getManifestEntryMap(manifest, Manifests.PROJECT_ATTRIBUTES.class);
                swaggerFeature.setTitle(projectInfo.get(Manifests.PROJECT_ATTRIBUTES.Title));
                swaggerFeature.setDescription(projectInfo.get(Manifests.PROJECT_ATTRIBUTES.Description));
                swaggerFeature.setLicense(projectInfo.get(Manifests.PROJECT_ATTRIBUTES.License));
                swaggerFeature.setLicenseUrl(projectInfo.get(Manifests.PROJECT_ATTRIBUTES.LicenseUrl));
                swaggerFeature.setVersion(projectInfo.get(Manifests.PROJECT_ATTRIBUTES.Version));
                swaggerFeature.setContact(projectInfo.get(Manifests.PROJECT_ATTRIBUTES.Contact));
            } catch (IOException e) {
                LOG.info("Could not read the project attributes from the Manifest due to " + e.getMessage());
            }
        }
        return new HashSet<Object>(
                Arrays.asList(
                        rootService,
                        customerService,
                        jacksonJsonProvider,
                        swaggerFeature,
                        new EnableJMXFeature(),
                        new LoggingFeature()
                )
        );
    }


}

