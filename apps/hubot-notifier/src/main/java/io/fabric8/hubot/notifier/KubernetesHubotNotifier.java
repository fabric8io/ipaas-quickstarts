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
import io.fabric8.hubot.HubotNotifier;
import io.fabric8.kubernetes.api.AbstractWatcher;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.Watcher;
import io.fabric8.kubernetes.api.builds.BuildWatcher;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.utils.Strings;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import static java.beans.Introspector.decapitalize;

/**
 * Watches the kubernetes resources in the current namespace and notifies hubot
 */
@ApplicationScoped
@Eager
public class KubernetesHubotNotifier {
    public static final String DEFAULT_ROOM_EXPRESSION = "#fabric8_${namespace}";

    private static final transient Logger LOG = LoggerFactory.getLogger(KubernetesHubotNotifier.class);
    private final HubotNotifier notifier;
    private final String consoleLink;
    private final String roomExpression;

    private KubernetesClient client = new KubernetesClient();
    private List<WebSocketClient> websocketClients = new ArrayList<>();

    /**
     * Note this constructor is only added to help CDI work with the {@link Eager} extension
     */
    public KubernetesHubotNotifier() {
        this.notifier = null;
        this.consoleLink = null;
        this.roomExpression = DEFAULT_ROOM_EXPRESSION;
    }

    @Inject
    public KubernetesHubotNotifier(HubotNotifier notifier,
                                   @External @Protocol("http") @ServiceName("fabric8") String consoleLink,
                                   @ConfigProperty(name = "HUBOT_KUBERNETES_ROOM", defaultValue = DEFAULT_ROOM_EXPRESSION) String roomExpression) throws Exception {
        this.notifier = notifier;
        this.consoleLink = consoleLink;
        this.roomExpression = roomExpression;

        LOG.info("Starting watching Kubernetes namespace " + getNamespace() + " at " + client.getAddress() + " using console link: " + consoleLink);

        addClient(client.watchServices(null, new AbstractWatcher<Service>() {
            @Override
            public void eventReceived(Action action, Service entity) {
                onWatchEvent(action, entity);
            }
        }));

        addClient(client.watchPods(null, new AbstractWatcher<Pod>() {
            @Override
            public void eventReceived(Action action, Pod entity) {
                onWatchEvent(action, entity);
            }
        }));

        addClient(client.watchReplicationControllers(null, new AbstractWatcher<ReplicationController>() {
            @Override
            public void eventReceived(Action action, ReplicationController entity) {
                onWatchEvent(action, entity);
            }
        }));

        addClient(client.watchBuilds(null, new AbstractWatcher<Build>() {
            @Override
            public void eventReceived(Action action, Build entity) {
                onWatchEvent(action, entity);
            }
        }));

        addClient(client.watchDeploymentConfigs(null, new AbstractWatcher<DeploymentConfig>() {
            @Override
            public void eventReceived(Action action, DeploymentConfig entity) {
                onWatchEvent(action, entity);
            }
        }));

        LOG.info("Now watching services, pods, replication controllers, builds and deployments");
    }

    public String getNamespace() {
        return client.getNamespace();
    }

    protected void addClient(WebSocketClient webSocketClient) {
        websocketClients.add(webSocketClient);
    }

    protected void onWatchEvent(Watcher.Action action, HasMetadata entity) {
        String kind = KubernetesHelper.getKind(entity);
        String name = KubernetesHelper.getName(entity);
        String namespace = getNamespace();

        String actionText = action.toString().toLowerCase();
        String room = this.roomExpression.replace("${namespace}", namespace).replace("${kind}", kind).replace("${name}", name);


        String postfix = "";
        if (action.equals(Watcher.Action.ADDED) || action.equals(Watcher.Action.MODIFIED)) {
            if (Strings.isNotBlank(consoleLink)) {
                postfix += " " + consoleLink + "/kubernetes/namespace/" + namespace + "/" + kind.toLowerCase() + "s/" + name;
            }
        }

        String message = actionText + " " + decapitalize(kind) + " " + name + postfix;
        notifier.notifyRoom(room, message);
    }
}
