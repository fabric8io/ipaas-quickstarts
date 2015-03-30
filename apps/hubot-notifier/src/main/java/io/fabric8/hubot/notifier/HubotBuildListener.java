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

import io.fabric8.kubernetes.api.builds.BuildFinishedEvent;
import io.fabric8.kubernetes.api.builds.BuildListener;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Notifies Hubot if we detect a build complete or fail
 */
public class HubotBuildListener implements BuildListener {
    private static final transient Logger LOG = LoggerFactory.getLogger(HubotBuildListener.class);

    private final HubotNotifier notifier;
    private final String room;

    @Inject
    public HubotBuildListener(HubotNotifier notifier,
                              @ConfigProperty(name = "BUILD_ROOM", defaultValue = "#fabric8-${namespace}") String room) {
        this.notifier = notifier;
        this.room = room;
    }

    @Override
    public void onBuildFinished(BuildFinishedEvent event) {
        String link = event.getBuildLink();
        String status = event.getStatus();
        String configName = event.getConfigName();
        String namespace = event.getNamespace();

        String message = "build " + status + " for " + namespace + "/" + configName + ": " + link;
        String room = createRoom(namespace, configName);

        notifier.notify(room, message);
    }

    protected String createRoom(String namespace, String configName) {
        return room.replace("${namespace}", namespace).replace("${buildConfig}", configName);
    }
}
