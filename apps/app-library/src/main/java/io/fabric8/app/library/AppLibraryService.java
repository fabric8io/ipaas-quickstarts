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
package io.fabric8.app.library;


import com.wordnik.swagger.annotations.Api;
import io.fabric8.app.library.support.AppDTO;
import io.fabric8.app.library.support.KubernetesService;
import io.fabric8.utils.IOHelpers;
import io.hawt.aether.AetherFacade;
import io.hawt.git.GitFacade;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * Represents the App Library REST API
 */
@Path("/")
@Api(value = "/", description = "App Library")
@Produces({"application/json", "text/xml"})
@Consumes({"application/json", "text/xml"})
public class AppLibraryService {
    private static final Logger LOG = LoggerFactory.getLogger(AppLibraryService.class);

    @Inject
    private GitFacade git;

    @Inject
    private AetherFacade aether;

    @Inject
    private KubernetesService kubernetesService;

    private MessageContext messageContext;
    private String urlPrefix;

    public AppLibraryService() {
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String index() throws IOException {
        URL resource = getClass().getResource("index.html");
        if (resource != null) {
            InputStream in = resource.openStream();
            if (in != null) {
                return IOHelpers.readFully(in);
            }
        }
        return null;
    }

    @GET
    @Path("index.html")
    @Produces(MediaType.TEXT_HTML)
    public String indexHtml() throws IOException {
        return index();
    }

    @GET
    @Path("_ping")
    public String ping() {
        return "true";
    }

    @GET
    @Path("apps")
    public Response podApis(@QueryParam("branch") String branch, @Context Request request) throws Exception {
        return kubernetesService.findAppsWithETags(branch, request);
    }

    @Context
    public void setMessageContext(MessageContext messageContext) {
        this.messageContext = messageContext;
    }
}
