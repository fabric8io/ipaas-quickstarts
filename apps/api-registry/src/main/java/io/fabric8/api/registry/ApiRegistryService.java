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
package io.fabric8.api.registry;


import com.google.common.io.Files;
import com.wordnik.swagger.annotations.Api;
import io.fabric8.utils.IOHelpers;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * Represents the API Registry REST API
 */
@Path("/")
@Api(value = "/", description = "Api Registry")
@Produces({"application/json", "text/xml"})
@Consumes({"application/json", "text/xml"})
public class ApiRegistryService {
    private static final Logger LOG = LoggerFactory.getLogger(ApiRegistryService.class);

    private MessageContext messageContext;

    public ApiRegistryService() {
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
    @Path("endpoints/pods")
    public List<ApiDTO> podServices(@QueryParam("selector") String selector) {
        ApiFinder finder = new ApiFinder();
        return finder.findServicesOnPods(selector);
    }

    @Context
    public void setMessageContext(MessageContext messageContext) {
        this.messageContext = messageContext;
    }

}
