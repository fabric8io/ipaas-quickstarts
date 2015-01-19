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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.utils.JMXUtils;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import io.hawt.kubernetes.KubernetesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import static io.fabric8.kubernetes.api.KubernetesHelper.getId;

/**
 * Provides an App view of all the services, controllers, pods
 */
public class AppView implements AppViewMXBean {
    private static final transient Logger LOG = LoggerFactory.getLogger(AppView.class);

    public static ObjectName OBJECT_NAME;

    private final KubernetesService kubernetesService;
    private final AtomicReference<AppViewSnapshot> snapshotCache = new AtomicReference<>();
    private long pollPeriod = 3000;
    private Timer timer = new Timer();
    private MBeanServer mbeanServer;

    static {
        try {
            OBJECT_NAME = new ObjectName("io.fabric8:type=AppView");
        } catch (MalformedObjectNameException e) {
            // ignore
        }
    }

    private KubernetesClient kubernetes = new KubernetesClient();
    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
            refreshData();
        }
    };

    @Inject
    public AppView(KubernetesService kubernetesService) {
        this.kubernetesService = kubernetesService;
        Objects.notNull(kubernetesService, "kubernetesService");
    }

    @PostConstruct
    public void init() {
        if (pollPeriod > 0) {
            timer.schedule(task, pollPeriod, pollPeriod);

            JMXUtils.registerMBean(this, OBJECT_NAME);
        }
    }

    @PreDestroy
    public void destroy() {
        if (timer != null) {
            timer.cancel();
        }
        JMXUtils.unregisterMBean(OBJECT_NAME);
    }

    @Override
    public String getKubernetesAddress() {
        return kubernetes.getAddress();
    }

    public long getPollPeriod() {
        return pollPeriod;
    }

    public void setPollPeriod(long pollPeriod) {
        this.pollPeriod = pollPeriod;
    }

    public AppViewSnapshot getSnapshot() {
        return snapshotCache.get();
    }

    public List<AppSummaryDTO> getAppSummaries() {
        List<AppSummaryDTO> answer = new ArrayList<>();
        AppViewSnapshot snapshot = getSnapshot();
        if (snapshot != null) {
            Collection<AppViewDetails> apps = snapshot.getApps();
            for (AppViewDetails app : apps) {
                AppSummaryDTO summary = app.getSummary();
                if (summary != null) {
                    answer.add(summary);
                }
            }
        }
        return answer;
    }

    @Override
    public String findAppSummariesJson() throws JsonProcessingException {
        return KubernetesHelper.toJson(getAppSummaries());
    }

    protected void refreshData() {
        try {
            AppViewSnapshot snapshot = createSnapshot();
            if (snapshot != null) {
                snapshotCache.set(snapshot);
            }
        } catch (Exception e) {
            LOG.warn("Failed to create snapshot: " + e, e);
        }
    }

    public AppViewSnapshot createSnapshot() throws Exception {
        Map<String, Service> servicesMap = KubernetesHelper.getServiceMap(kubernetes);
        Map<String, ReplicationController> controllerMap = KubernetesHelper.getReplicationControllerMap(kubernetes);
        Map<String, Pod> podMap = KubernetesHelper.getPodMap(kubernetes);

        AppViewSnapshot snapshot = new AppViewSnapshot(servicesMap, controllerMap, podMap);
        for (Service service : servicesMap.values()) {
            String appPath = getAppPath(getId(service));
            if (appPath != null) {
                AppViewDetails dto = snapshot.getOrCreateAppView(appPath, service.getNamespace());
                dto.addService(service);
            }
        }
        for (ReplicationController controller : controllerMap.values()) {
            String appPath = getAppPath(getId(controller));
            if (appPath != null) {
                AppViewDetails dto = snapshot.getOrCreateAppView(appPath, controller.getNamespace());
                dto.addController(controller);
            }
        }

        // lets add any missing RCs
        Set<ReplicationController> remainingControllers = new HashSet<>(controllerMap.values());
        Collection<AppViewDetails> appViews = snapshot.getApps();
        for (AppViewDetails appView : appViews) {
            remainingControllers.removeAll(appView.getControllers().values());
        }

        for (ReplicationController controller : remainingControllers) {
            AppViewDetails dto = snapshot.createApp(controller.getNamespace());
            dto.addController(controller);
        }

        // lets add any missing pods
        Set<Pod> remainingPods = new HashSet<>(podMap.values());
        for (AppViewDetails appView : appViews) {
            remainingPods.removeAll(appView.getPods().values());
        }
        for (Pod pod : remainingPods) {
            AppViewDetails dto = snapshot.createApp(pod.getNamespace());
            dto.addPod(pod);
        }

        snapshotCache.set(snapshot);
        return snapshot;
    }

    /**
     * Returns the App path for the given kubernetes service or controller id or null if it cannot be found
     */
    protected String getAppPath(String serviceId) throws Exception {
        if (Strings.isNullOrBlank(serviceId)) {
            return null;
        }
        String branch = "master";
        return kubernetesService.appPath(branch, serviceId);
    }

    public MBeanServer getMBeanServer() {
        if (mbeanServer == null) {
            mbeanServer = ManagementFactory.getPlatformMBeanServer();
        }
        return mbeanServer;
    }

    public void setMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }
}
