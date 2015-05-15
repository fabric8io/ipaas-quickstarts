/*
 *
 *  * Copyright 2005-2015 Red Hat, Inc.
 *  * Red Hat licenses this file to you under the Apache License, version
 *  * 2.0 (the "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  * implied.  See the License for the specific language governing
 *  * permissions and limitations under the License.
 *
 */

package io.fabric8.mq.coordination.scaling;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import io.fabric8.mq.model.Model;
import org.easyrules.core.BasicRule;

import java.util.HashMap;
import java.util.Map;

public abstract class ScalingRule extends BasicRule implements MetricSet, ScalingRuleMBean {
    protected final Model model;
    protected final ScalingEngine scalingEngine;
    private final String name;
    private int priority;
    private String description;
    private Map<String, Metric> metricMap = new HashMap<>();
    private Meter executed;
    private Counter called;

    public ScalingRule(ScalingEngine scalingEngine, String name, String description, int priority) {
        this.scalingEngine = scalingEngine;
        this.model = scalingEngine.getModel();
        this.name = name;
        this.description = description;
        this.priority = priority;
        String str = getClass().getPackage().getName() + ".rule." + name;
        executed = new Meter();
        called = new Counter();
        metricMap.put(str + ".executed", executed);
        metricMap.put(str + ".called", called);
        Model.METRIC_REGISTRY.registerAll(this);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getName() {
        return name;
    }

    public Map<String, Metric> getMetrics() {
        return metricMap;
    }

    protected void called() {
        called.inc();
    }

    protected void executed() {
        executed.mark();
    }

}
