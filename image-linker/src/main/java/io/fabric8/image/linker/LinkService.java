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
package io.fabric8.image.linker;


import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

@Path("/")
@Singleton
public class LinkService {
    private static final Logger LOG = LoggerFactory.getLogger(LinkService.class);

    public LinkService() {
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
    @Path("{protocol}/{host}/{port}/p/{path: .*}.png")
    public Response image(@PathParam("protocol") String protocol, @PathParam("host") String host, @PathParam("port") String port, @PathParam("path") String path, @Context UriInfo uriInfo) throws URISyntaxException {
        String queryArgs = uriInfo.getRequestUri().getQuery();
        return renderImage(protocol, host, port, path, queryArgs);
    }

    @GET
    @Path("{protocol}/{host}/{port}/q/{query}/{path: .*}.png")
    public Response imageWithEmbeddedQuery(@PathParam("protocol") String protocol, @PathParam("host") String host, @PathParam("port") String port, @PathParam("path") String path, @PathParam("query") String query) throws URISyntaxException {
        return renderImage(protocol, host, port, path, query);
    }

    protected Response renderImage(@PathParam("protocol") String protocol, @PathParam("host") String host, @PathParam("port") String port, @PathParam("path") String path, String queryArgs) throws URISyntaxException {
        String query = "";
        if (Strings.isNotBlank(queryArgs)) {
            query = "?" + queryArgs;
        }
        String portText = port != null && port.length() > 0 ? ":" + port : "";
        String urlText = protocol + "://" + host + portText + "/" + path + query;
        System.out.println("URL text: " + urlText);
        URI uri = new URI(urlText);
        if (shouldRedirect()) {
            System.out.println("Sending to URI: " + uri);
            return Response.temporaryRedirect(uri).build();
        } else {
            Client client = ClientBuilder.newClient();
            Response response = client.target(uri).request().get();
            MultivaluedMap<String, Object> headers = response.getHeaders();
            System.out.println("Headers: " + headers);
            return response;
        }
    }

    /**
     * Should we redirect or just proxy to the back end service
     * @return
     */
    protected boolean shouldRedirect() {
        return false;
    }

}
