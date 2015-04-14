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
package io.fabric8.cdelivery;

import com.wordnik.swagger.annotations.Api;
import io.fabric8.cdelivery.support.BuildTriggerDTO;
import io.fabric8.io.fabric8.workflow.build.trigger.BuildTrigger;
import io.fabric8.io.fabric8.workflow.build.trigger.BuildTriggers;
import io.fabric8.io.fabric8.workflow.build.trigger.BuildWorkItemHandler;
import io.fabric8.utils.IOHelpers;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Represents the App Library REST API
 */
@Path("/")
@Api(value = "/", description = "CDelivery")
@Produces({"application/json", "text/xml"})
@Consumes({"application/json", "text/xml"})
public class CDeliveryService {
    private static final Logger LOG = LoggerFactory.getLogger(CDeliveryService.class);

    private MessageContext messageContext;
    private String urlPrefix;

    public CDeliveryService() {
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
    @Path("_ping")
    public String ping() {
        return "true";
    }


    @GET
    @Path("index.html")
    @Produces(MediaType.TEXT_HTML)
    public String indexHtml() throws IOException {
        return index();
    }

    /**
     * Triggers the build from the given {@link io.fabric8.cdelivery.support.BuildTriggerDTO}
     *
     * @return the newly created build UUID
     * @throws Exception
     */
    @POST
    @Path("triggerBuild")
    @Produces("text/plain")
    public Response triggerBuild(BuildTriggerDTO triggerDto, @Context Request request) throws Exception {
        BuildWorkItemHandler handler = new BuildWorkItemHandler();
        Long processInstanceId = triggerDto.getProcessInstanceId();
        Long workItemId = triggerDto.getWorkItemId();
        String namespace = triggerDto.getNamespace();
        String buildName = triggerDto.getBuildName();

        StringBuilder errors = new StringBuilder();
        checkNotNullProperty(errors, processInstanceId, "processInstanceId");
        checkNotNullProperty(errors, workItemId, "workItemId");
        checkNotNullProperty(errors, namespace, "namespace");
        checkNotNullProperty(errors, buildName, "buildName");

        String errorMessage = errors.toString();
        if (errorMessage.isEmpty()) {
            String buildUuid = handler.triggerBuild(processInstanceId, workItemId, namespace, buildName);
            return Response.ok(buildUuid).build();
        } else {
            return Response.status(422).entity("Missing " + errorMessage).build();
        }
    }


    /**
     * A proxy replacement for the standard OpenShift build trigger REST API as its currently
     * kinda flaky and barfs if you pass any headers
     */
    @POST
    @Path("buildConfigHooks/{namespace}/{name}")
    @Produces("text/plain")
    @Consumes("*/*")
    public Response triggerBuildProxy(@PathParam("namespace") String namespace, @PathParam("name") String name, @Context Request request) throws Exception {
        BuildTrigger buildTrigger = BuildTriggers.getSingleton();
        LOG.info("Invoking build on namespace: " + namespace + " and buildConfig name: " + name);
        String buildUuid = buildTrigger.trigger(namespace, name);
        LOG.info("Build on namespace: " + namespace + " and buildConfig name: " + name + " generated uuid: " + buildUuid);
        return Response.ok(buildUuid).build();
    }

    protected void checkNotNullProperty(StringBuilder errors, Object value, String name) {
        if (value == null) {
            if (errors.length() > 0) {
                errors.append(", ");
            }
            errors.append(name);
        }
    }

    @Context
    public void setMessageContext(MessageContext messageContext) {
        this.messageContext = messageContext;
    }
}
