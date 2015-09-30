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
import java.io.InputStream;
import java.net.URL;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.fabric8.utils.IOHelpers;
import io.swagger.annotations.Api;

/**
 * This Java class with be hosted in the URI path defined by the @Path annotation. @Path annotations on the methods
 * of this class always refer to a path relative to the path defined at the class level.
 * <p/>
 * For example, with 'http://localhost:8181/cxf' as the default CXF servlet path and '/crm' as the JAX-RS server path,
 * this class will be hosted in 'http://localhost:8181/cxf/crm/customerservice'.  An @Path("/customers") annotation on
 * one of the methods would result in 'http://localhost:8181/cxf/crm/customerservice/customers'.
 */
@Path("/")
@Api(value = "/", description = "CXF CDI Quickstart")
public class RootService {

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

    public RootService() {
    }

    @GET
    @Path("_ping")
    public String ping() {
        return "true";
    }


}
