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

import io.fabric8.app.library.support.AppSummaryDTO;
import io.fabric8.app.library.support.AppView;
import io.fabric8.app.library.support.AppViewDetails;
import io.fabric8.app.library.support.AppViewSnapshot;
import io.fabric8.app.library.support.HawtioProducers;
import io.fabric8.app.library.support.NamespaceAndAppPath;
import io.hawt.aether.AetherFacade;
import io.hawt.git.GitFacade;

import java.util.List;
import java.util.Map;

/**
 * Applies the given JSON file to the kubernetes environment
 */
public class ViewAppSummaries {
    public static void main(String... args) {
        AppView appView = null;
        HawtioProducers producers = new HawtioProducers();
        try {
            appView = new AppView(producers.createKubernetesService());

            String fabric8Version = getFabric8Version();
            GitFacade git = producers.createGit("mvn:io.fabric8.quickstarts/fabric8-quickstarts-parent/" + fabric8Version
                    + "/zip/app,mvn:io.fabric8.jube.images.fabric8/apps/" + fabric8Version + "/zip/app");
            AetherFacade aether = producers.createAether();
            boolean testTimer = false;

            appView.init();

            System.out.println("Querying kubernetes at: " + appView.getKubernetesAddress());

            AppViewSnapshot snapshot = null;
            if (testTimer) {
                for (int i = 0; i < 20; i++) {
                    snapshot = appView.getSnapshot();
                    if (snapshot != null) {
                        break;
                    } else {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    }
                }
            } else {
                snapshot = appView.createSnapshot();
            }
            System.out.println("Snapshot: " + snapshot);
            if (snapshot == null) {
                return;
            }
            Map<NamespaceAndAppPath, AppViewDetails> apps = snapshot.getAppMap();
            System.out.println("Services: " + snapshot.getServicesMap().size());
            System.out.println("Controllers: " + snapshot.getControllerMap().size());
            System.out.println("Pods: " + snapshot.getPodMap().size());
            System.out.println("Has running apps: " + apps.keySet());
            List<AppSummaryDTO> appSummaries = appView.getAppSummaries();
            for (AppSummaryDTO appSummary : appSummaries) {
                System.out.println("app: " + appSummary);
            }
            System.out.println("JSON: " + appView.findAppSummariesJson());
        } catch (Exception e) {
            System.out.println("FAILED: " + e);
            e.printStackTrace();
        } finally {
            if (appView != null) {
                try {
                    appView.destroy();
                } catch (Exception e) {
                    System.out.println("FAILED: " + e);
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getFabric8Version() {
        return System.getProperty("fabric8.version", "2.0.15");
    }

    public static String getBasedir() {
        return System.getProperty("basedir", ".");
    }
}
