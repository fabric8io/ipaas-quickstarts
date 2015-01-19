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
package io.fabric8.app.library.support;

import io.fabric8.utils.Files;
import io.hawt.aether.AetherFacade;
import io.hawt.git.GitFacade;
import io.hawt.kubernetes.KubernetesService;
import org.apache.deltaspike.core.api.config.ConfigProperty;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import java.io.File;

/**
 */
public class HawtioProducers {
    @Produces
    @Singleton
    public GitFacade createGit(@ConfigProperty(name = "IMPORT_APP_URLS") String importUrls) throws Exception {
        GitFacade git = new GitFacade();
        System.out.println("Importing urls: " + importUrls);
        git.setInitialImportURLs(importUrls);
        git.setCloneRemoteRepoOnStartup(false);
        File configDir = new File("libraryConfig");
        if (configDir.exists()) {
            Files.recursiveDelete(configDir);
        }
        configDir.mkdirs();

        git.setConfigDirectory(configDir);
        git.init();

        System.out.println("Created library at at: " + configDir.getAbsolutePath());
        return git;
    }

    @Produces
    @Singleton
    public AetherFacade createAether() throws Exception {
        AetherFacade aether = new AetherFacade();
        aether.init();
        return aether;
    }

    @Produces
    @Singleton
    public KubernetesService createKubernetesService() throws Exception {
        KubernetesService answer = new KubernetesService();
        answer.init();
        return answer;
    }

}
