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
package io.fabric8.api.registry;


import com.wordnik.swagger.annotations.Api;
import io.fabric8.api.registry.rules.SwaggerHelpers;
import io.fabric8.swagger.model.ApiDeclaration;
import io.fabric8.swagger.model.Api_;
import io.fabric8.swagger.model.Authorizations_;
import io.fabric8.swagger.model.InfoObject;
import io.fabric8.swagger.model.ResourceListing;
import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static io.fabric8.utils.URLUtils.urlPathJoin;

/**
 * Represents the API Registry REST API
 */
@Path("/")
@Api(value = "/", description = "Api Registry")
@Produces({"application/json", "text/xml"})
@Consumes({"application/json", "text/xml"})
public class ApiRegistryService {
    private static final Logger LOG = LoggerFactory.getLogger(ApiRegistryService.class);

    @Inject
    private ApiFinder finder;

    private MessageContext messageContext;
    private String urlPrefix;

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
    public List<ApiDTO> podApis(@QueryParam("selector") String selector) {
        return getFinder().findApisOnPods(selector);
    }

    protected ApiFinder getFinder() {
        checkForUrlPrefix();
        return finder;
    }

    @GET
    @Path("endpoints/services")
    public List<ApiDTO> serviceApis(@QueryParam("selector") String selector) {
        return getFinder().findApisOnServices(selector);
    }

    @GET
    @Path("swagger/pod/{pod}/{container}")
    public ResourceListing swagger(@PathParam("pod") String pod, @PathParam("container") String container) {
        ApiDeclaration apiDeclaration = rootSwagger(pod, container);
        if (apiDeclaration == null) {
            return null;
        }
        ResourceListing listing = new ResourceListing();
        ApiDeclaration.SwaggerVersion swaggerVersion = apiDeclaration.getSwaggerVersion();
        if (swaggerVersion != null) {
            listing.setSwaggerVersion(ResourceListing.SwaggerVersion.fromValue(swaggerVersion.toString()));
        } else {
            listing.setSwaggerVersion(ResourceListing.SwaggerVersion._1_2);
        }
        listing.setApiVersion(apiDeclaration.getApiVersion());
        listing.setAuthorizations(new Authorizations_());
        InfoObject info = new InfoObject();
        info.setTitle("REST API for Apache Camel");
        listing.setInfo(info);
        List<Api_> apis = createResourceApis(apiDeclaration);
        listing.setApis(apis);
        return listing;
    }

    protected static List<Api_> createResourceApis(ApiDeclaration apiDeclaration) {
        List<Api_> answer = new ArrayList<>();
        String resourcePath = apiDeclaration.getResourcePath();
        if (Strings.isNotBlank(resourcePath)) {
            Api_ listing = new Api_();
            try {
                listing.setPath(new URI(resourcePath));
                answer.add(listing);
            } catch (URISyntaxException e) {
                System.out.println("Failed to convert " + resourcePath + " to a URI. " + e);
                LOG.warn("Failed to convert " + resourcePath + " to a URI. " + e, e);
            }
        }
        return answer;
    }

    protected ApiDeclaration rootSwagger(String pod, String container) {
        Objects.notNull(pod, "pod");
        Objects.notNull(container, "container");
        PodAndContainerId key = new PodAndContainerId(pod, container);
        return getFinder().getSwaggerForPodAndContainer(key);
    }

    @GET
    @Path("swagger/pod/{pod}/{container}/{path:.*}")
    public ApiDeclaration swagger(@PathParam("pod") String pod, @PathParam("container") String container, @PathParam("path") String path) {
        ApiDeclaration swagger = rootSwagger(pod, container);
        if (Strings.isNotBlank(path)) {
            String pathPrefix = urlPathJoin("/", path);
            ApiDeclaration filtered = new ApiDeclaration();
            try {
                BeanUtils.copyProperties(filtered, swagger);
            } catch (Exception e) {
                LOG.info("Failed to copy properties: " + e, e);
                return filtered;
            }
            filtered.setApis(SwaggerHelpers.filterApis(swagger, pathPrefix));
            return filtered;
        }
        return swagger;
    }

    @Context
    public void setMessageContext(MessageContext messageContext) {
        this.messageContext = messageContext;
        finder.setMessageContext(messageContext);
        checkForUrlPrefix();
    }

    protected void checkForUrlPrefix() {
        if (urlPrefix == null && messageContext != null) {
            HttpServletRequest request = messageContext.getHttpServletRequest();
            ServletContext servletContext = messageContext.getServletContext();
            if (request != null && servletContext != null) {
                String swaggerPrefix = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
                String contextPath = servletContext.getContextPath();
                urlPrefix = urlPathJoin(swaggerPrefix, contextPath);
                finder.setUrlPrefix(urlPrefix);
            }
        }

    }

}
