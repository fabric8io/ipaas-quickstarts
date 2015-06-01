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
package io.fabric8.hubot.notifier;

import io.fabric8.annotations.Eager;
import io.fabric8.annotations.External;
import io.fabric8.annotations.Protocol;
import io.fabric8.annotations.ServiceName;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.builds.BuildWatcher;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Timer;

/**
 * A service which listens to {@link io.fabric8.kubernetes.api.builds.BuildFinishedEvent} events from
 * the OpenShift build watcher and then signals the correlated jBPM process instances or
 * signals new processes to start.
 * <p/>
 * This service is a helper class to create a configured instance of a
 * {@link io.fabric8.io.fabric8.workflow.build.signal.BuildSignaller} using the
 * {@link io.fabric8.kubernetes.api.builds.BuildWatcher} helper class.
 */
@ApplicationScoped
@Eager
public class BuildWatchService {
    private static final transient Logger LOG = LoggerFactory.getLogger(BuildWatchService.class);
    private static final long DEFAULT_POLL_TIME = 3000;

    private final String consoleLink;
    private final String namespace;
    private final BuildWatcher watcher;
    private KubernetesClient client = new KubernetesClient();
    private Timer timer = new Timer();

    /**
     * Note this constructor is only added to help CDI work with the {@link Eager} extension
     */
    public BuildWatchService() {
        // lets do nothing!
        consoleLink = null;
        namespace = null;
        watcher = null;
    }

    @Inject
    public BuildWatchService(@External @Protocol("http") @ServiceName("fabric8") String consoleLink,
                             @ConfigProperty(name = "BUILD_NAMESPACE", defaultValue = "") String namespace,
                             @ConfigProperty(name = "BUILD_POLL_TIME", defaultValue = "" + DEFAULT_POLL_TIME) long pollTime,
                             HubotBuildListener buildListener) {

        LOG.info("Starting Hubot BuildWatchService using console link: " + consoleLink);

        if (pollTime <= 0) {
            LOG.warn("Invalid poll time " + pollTime + " so using default value of " + DEFAULT_POLL_TIME + " instead");
            pollTime = DEFAULT_POLL_TIME;
        }
        if (namespace != null && namespace.trim().length() == 0) {
            namespace = null;
        }
        this.namespace = namespace;
        this.consoleLink = consoleLink;

        watcher = new BuildWatcher(client, buildListener, namespace, consoleLink);
        watcher.schedule(timer, pollTime);
    }
}
