/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.chaos.monkey;

import io.fabric8.annotations.Eager;
import io.fabric8.hubot.HubotNotifier;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.utils.Filter;
import io.fabric8.utils.Filters;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

/**
 * Implements a Chaos Monkey which is a kubernetes port of the <a href="https://github.com/Netflix/SimianArmy/wiki/Chaos-Monkey">Netflix Chaos Monkey</a>
 */
@ApplicationScoped
@Eager
public class ChaosMonkey {
    public static final String DEFAULT_ROOM_EXPRESSION = "#fabric8_${namespace}";
    public static final String DEFAULT_INCLUDES = "";
    public static final String DEFAULT_EXCLUDES = "letschat*,gogs*";

    private static final transient Logger LOG = LoggerFactory.getLogger(ChaosMonkey.class);
    private TimerTask task;
    private HubotNotifier notifier;
    private final String roomExpression;
    private final String includePatterns;
    private final String excludePatterns;
    private final int killFrequency;
    private Filter<String> includeFilter;
    private Filter<String> excludeFilter;

    private KubernetesClient kubernetes = new KubernetesClient();
    private Timer timer = new Timer("Chaos Monkey timer", true);

    /**
     * Note this constructor is only added to help CDI work with the {@link Eager} extension
     */
    public ChaosMonkey() {
        roomExpression = DEFAULT_ROOM_EXPRESSION;
        includePatterns = DEFAULT_INCLUDES;
        excludePatterns = DEFAULT_EXCLUDES;
        killFrequency = 60;
    }

    @Inject
    public ChaosMonkey(HubotNotifier notifier,
                       @ConfigProperty(name = "CHAOS_MONKEY_ROOM", defaultValue = DEFAULT_ROOM_EXPRESSION) String roomExpression,
                       @ConfigProperty(name = "CHAOS_MONKEY_INCLUDES", defaultValue = DEFAULT_INCLUDES) String includePatterns,
                       @ConfigProperty(name = "CHAOS_MONKEY_EXCLUDES", defaultValue = DEFAULT_EXCLUDES) String excludePatterns,
                       @ConfigProperty(name = "CHAOS_MONKEY_KILL_FREQUENCY_SECONDS", defaultValue = "60") int killFrequency) throws Exception {
        this.notifier = notifier;
        this.roomExpression = roomExpression;
        this.includePatterns = includePatterns;
        this.excludePatterns = excludePatterns;
        this.killFrequency = killFrequency;

        this.includeFilter = createTextFilter(includePatterns);
        this.excludeFilter = createTextFilter(excludePatterns);

        if (killFrequency < 1) {
            LOG.warn("Ignoring invalid killFrequency of " + killFrequency);
            killFrequency = 60;
        }

        LOG.info("Starting Chaos Monkey on Kubernetes namespace " + getNamespace() + " at " + kubernetes.getAddress() + " with includes " + includePatterns + " excludes " + excludePatterns + " " + " kill frequency " + killFrequency + " seconds");

        notify("Chaos Monkey is starting in namespace " + getNamespace() + " with include patterns '" + includePatterns + "' exclude patterns '" + excludePatterns + "' and a kill frequency of " + killFrequency + " seconds. Here I come!");

        task = new TimerTask() {
            @Override
            public void run() {
                killPod();
            }
        };
        long period = killFrequency * 1000;
        long initialDelay = 1;
        timer.schedule(task, initialDelay, period);
        waitUntilCompleted();
    }

    public static Filter<String> createTextFilter(String patterns) {
        if (patterns != null && patterns.contains(",")) {
            String[] split = patterns.split(",");
            List<String> array = Arrays.asList(split);
            return Filters.createStringFilters(array);
        } else {
            return Filters.createStringFilter(patterns);
        }
    }

    protected void waitUntilCompleted() {
        CountDownLatch latch = new CountDownLatch(1);
        while (true) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    public String getNamespace() {
        return kubernetes.getNamespace();
    }

    protected void killPod() {
        String namespace = getNamespace();
        PodList pods = kubernetes.getPods(namespace);
        List<Pod> targets = new ArrayList<>();
        if (pods != null) {
            List<Pod> podList = pods.getItems();
            for (Pod pod : podList) {
                if (isTarget(pod)) {
                    targets.add(pod);
                }
            }
        }
        String message = null;
        Pod pod = null;
        if (targets.size() > 0) {
            pod = pickRandom(targets);
        }
        boolean killed = false;
        if (pod == null) {
            message = "No matching pods available to kill. Boo!";
        } else {
            String name = KubernetesHelper.getName(pod);
            try {
                kubernetes.deletePod(pod);
                message = "Chaos Monkey killed pod " + name + " in namespace " + namespace;
                killed = true;
            } catch (Exception e) {
                message = "Chaos Monkey failed to kill pod " + name + " in namespace " + namespace + " due to: " + e;
            }
        }
        notify(message);
        if (killed) {
            String monkey = getMonkey();
            if (monkey != null) {
                notify(monkey);
            }
        }
    }

    /**
     * Returns a random selection from the list
     */
    public static <T> T pickRandom(List<T> list) {
        int size = list.size();
        if (size <= 0) {
            return null;
        } else {
            int idx = (int) Math.round(Math.random() * (size - 1));
            return list.get(idx);
        }
    }

    protected String getMonkey() {
        String[] monkeys = {
                "http://i.giphy.com/yD9vMV7aDl3xK.gif",
                "http://i.giphy.com/ncBhyy0TVZHmU.gif",
                "http://i.giphy.com/12oyZr7VXoTn68.gif",
                "http://i.giphy.com/43nZCD3lLbri0.gif",
                "http://i.giphy.com/M93ZgxJzIV8v6.gif",
                "http://i.giphy.com/bLHJ71uLsgqWI.gif",
                "http://i.giphy.com/5Zesu5VPNGJlm.gif",
                "http://i.giphy.com/ava8sWgcW387C.gif",
                "http://i.giphy.com/dchERAZ73GvOE.gif",
                "http://i.giphy.com/pFwRzOLfuGHok.gif",
                "http://i.giphy.com/VH3X9TU7aQLrq.gif",
                "http://i.giphy.com/1Ia8zGu9QtH2w.gif",
                "http://i.giphy.com/2Faz9JzRpalvpYKGc.gif",
                "http://i.giphy.com/2ymva1ROJjoEU.gif",
                "http://i.giphy.com/TLulTJKuyLgMU.gif",
                "http://i.giphy.com/fw4EIdDeVvjna.gif",
                "http://i.giphy.com/OYJ2kbvdTPW6I.gif",
                "http://i.giphy.com/BBkKEBJkmFbTG.gif",
                "http://i.giphy.com/KqN5Nw9TuBJWE.gif",
                "http://i.giphy.com/yLZQKurQvmIAo.gif",
                "http://i.giphy.com/vIyZvUXy7O7XW.gif",
                "http://i.giphy.com/13nRKeBVhrAqmk.gif",
                "http://i.giphy.com/5YWrACKqcsPcI.gif",
                "http://i.giphy.com/kfse8uuYqn30c.gif",
                "http://i.giphy.com/Aak2oJcN9BS1y.gif",
                "http://i.giphy.com/AwXAC8MMX1JUk.gif",
                "http://i.giphy.com/rEEYzhbkalKLK.gif",
                "http://i.giphy.com/7RVJ5arTndqvK.gif",
                "http://i.giphy.com/j7Ol3sUfRY39S.gif",
                "http://i.giphy.com/CKZHnkRNIcr8A.gif"
        };
        return pickRandom(Arrays.asList(monkeys));

    }

    protected void notify(String message) {
        if (notifier == null) {
            LOG.warn("No notifier so can't say: " + message);
        } else {
            String room = getRoom();
            notifier.notifyRoom(room, message);
        }
    }

    /**
     * Returns true if the given pod matches the selection criteria
     */
    public boolean isTarget(Pod pod) {
        String name = KubernetesHelper.getName(pod);
        return includeFilter.matches(name) && !excludeFilter.matches(name);
    }

    protected String getRoom() {
        String namespace = getNamespace();
        return this.roomExpression.replace("${namespace}", namespace);
    }
}
