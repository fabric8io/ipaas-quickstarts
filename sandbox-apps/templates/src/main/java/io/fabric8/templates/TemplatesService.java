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
package io.fabric8.templates;


import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.extensions.Templates;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigList;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.api.model.TemplateList;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents the OpenShift Templates REST API
 */
@Path("/oapi/v1/namespaces/{namespace}")
@Produces({"application/json", "text/xml"})
@Consumes({"application/json", "text/xml"})
@Singleton
public class TemplatesService {
    private static final Logger LOG = LoggerFactory.getLogger(TemplatesService.class);

    private String dataFolder = Systems.getEnvVar("DATA_DIR", ".data");

    public TemplatesService() {
        System.out.println("Starting TemplateService using dataFolder " + dataFolder);
        LOG.info("Starting TemplateService using dataFolder " + dataFolder);
    }

    @POST
    @Path("processedtemplates")
    @Consumes({"application/json", "application/yaml"})
    public String processTemplate(@PathParam("namespace") String namespace, Template entity) throws Exception {
        KubernetesList list = Templates.processTemplatesLocally(entity, false);
        return KubernetesHelper.toJson(list);
    }


    // Template resource
    //-------------------------------------------------------------------------

    @GET
    @Path("templates")
    public Object getTemplates(@PathParam("namespace") String namespace, @QueryParam("watch") String watchFlag, @HeaderParam("Sec-WebSocket-Key") String reqid) {
        if (watchFlag != null && watchFlag.equals("true")) {
            // TODO
            // return monitorTemplates(reqid);
        }
        List<Template> items = getResourceList(namespace, Template.class);
        TemplateList answer = new TemplateList();
        answer.setItems(items);
        return answer;
    }

    @Path("templates")
    @POST
    @Consumes({"application/json", "application/yaml"})
    public void createTemplate(@PathParam("namespace") String namespace, Template entity) throws Exception {
        String name = KubernetesHelper.getName(entity);
        updateNamed(namespace, name, entity);
    }


    @GET
    @Path("templates/{name}")
    @Produces("application/json")
    public Template getTemplate(@PathParam("namespace") String namespace, @PathParam("name") @NotNull String name) {
        return findNamed(getResourceList(namespace, Template.class), name);
    }

    @PUT
    @Path("templates/{name}")
    @Consumes({"application/json", "application/yaml"})
    public void updateTemplate(@PathParam("namespace") String namespace, @PathParam("name") @NotNull String name, Template entity) throws IOException {
        updateNamed(namespace, name, entity);
    }

    @DELETE
    @Path("templates/{name}")
    @Produces("application/json")
    @Consumes("text/plain")
    public void deleteTemplate(@PathParam("namespace") String namespace, @PathParam("name") @NotNull String name) {
        deleteNamed(namespace, name, Template.class);
    }


    // BuildConfig resource
    //-------------------------------------------------------------------------

    @GET
    @Path("buildconfigs")
    public BuildConfigList getBuildConfigs(@PathParam("namespace") String namespace) {
        List<BuildConfig> items = getResourceList(namespace, BuildConfig.class);
        BuildConfigList answer = new BuildConfigList();
        answer.setItems(items);
        return answer;
    }

    @Path("buildconfigs")
    @POST
    @Consumes({"application/json", "application/yaml"})
    public void createBuildConfig(@PathParam("namespace") String namespace, BuildConfig entity) throws Exception {
        String name = KubernetesHelper.getName(entity);
        updateNamed(namespace, name, entity);
    }


    @GET
    @Path("buildconfigs/{name}")
    @Produces("application/json")
    public BuildConfig getBuildConfig(@PathParam("namespace") String namespace, @PathParam("name") @NotNull String name) {
        return findNamed(getResourceList(namespace, BuildConfig.class), name);
    }

    @PUT
    @Path("buildconfigs/{name}")
    @Consumes({"application/json", "application/yaml"})
    public void updateBuildConfig(@PathParam("namespace") String namespace, @PathParam("name") @NotNull String name, BuildConfig entity) throws IOException {
        updateNamed(namespace, name, entity);
    }

    @DELETE
    @Path("buildconfigs/{name}")
    @Produces("application/json")
    @Consumes("text/plain")
    public void deleteBuildConfig(@PathParam("namespace") String namespace, @PathParam("name") @NotNull String name) {
        deleteNamed(namespace, name, BuildConfig.class);
    }


    // Implementation methods
    //-------------------------------------------------------------------------

    protected <T extends HasMetadata> T findNamed(List<T> items, String name) {
        for (T item : items) {
            ObjectMeta metadata = item.getMetadata();
            if (metadata != null) {
                String aName = metadata.getName();
                if (Objects.equal(name, aName)) {
                    return item;
                }
            }
        }
        return null;
    }

    protected <T extends HasMetadata> List<T> getResourceList(@PathParam("namespace") String namespace, Class<T> clazz) {
        File folder = getResourceCollectionFolder(namespace, clazz);
        return loadFiles(folder, clazz);
    }

    /**
     * Loads all the files in the given folder
     */
    protected <T extends HasMetadata> List<T> loadFiles(File folder, Class<T> clazz) {
        List<T> answer = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName().toLowerCase();
                if (file.isFile() && name.endsWith(".json")) {
                    Object value = null;
                    try {
                        value = KubernetesHelper.loadJson(file);
                    } catch (IOException e) {
                        LOG.warn("Failed to load JSON file " + file + ". " + e, e);
                    }
                    if (value != null) {
                        if (clazz.isInstance(value)) {
                            T cast = clazz.cast(value);
                            answer.add(cast);
                        } else {
                            LOG.warn("Ignoring instance " + value + " of type " + value.getClass().getName() + " when expecting instance of " + clazz.getName() + " from file " + file);
                        }
                    }
                }
            }
        }
        return answer;
    }

    protected void deleteNamed(String namespace, String name, Class<? extends HasMetadata> clazz) {
        File entityFile = getResourceFile(namespace, name, clazz);
        if (entityFile != null && entityFile.exists()) {
            entityFile.delete();
        } else {
            throw new WebApplicationException("No such entity", Response.Status.NOT_FOUND);
        }
    }


    protected File getResourceFile(String namespace, String name, Class<? extends HasMetadata> clazz) {
        if (Strings.isNullOrBlank(name)) {
            return null;
        }
        File folder = getResourceCollectionFolder(namespace, clazz);
        return new File(folder, name + ".json");
    }

    protected <T extends HasMetadata> void updateNamed(String namespace, String name, T entity) throws IOException {
        File entityFile = getResourceFile(namespace, name, entity.getClass());
        ObjectMeta metadata = KubernetesHelper.getOrCreateMetadata(entity);
        metadata.setNamespace(namespace);
        metadata.setName(name);
        if (entityFile != null) {
            KubernetesHelper.saveJson(entityFile, entity);
            // TODO sendEvent(entity);
        } else {
            throw new WebApplicationException("No metadata.name supplied!");
        }
    }




    /**
     * Returns the data folder for the given namespace and data type
     */
    private File getResourceCollectionFolder(String namespace, Class<? extends HasMetadata> clazz) {
        String path = clazz.getSimpleName().toLowerCase();
        File data = new File(dataFolder);
        File namespaceFolder = new File(data, namespace);
        File answer = new File(namespaceFolder, path);
        answer.mkdirs();
        return answer;
    }


}
