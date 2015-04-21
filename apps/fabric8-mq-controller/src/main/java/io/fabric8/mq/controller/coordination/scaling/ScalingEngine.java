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

package io.fabric8.mq.controller.coordination.scaling;

import io.fabric8.mq.controller.model.Model;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.easyrules.jmx.DefaultJMXRulesEngine;
import org.easyrules.jmx.api.JMXRulesEngine;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ScalingEngine extends ServiceSupport{

    @Inject
    @ConfigProperty(name = "SCALE_UP_RULE_PRIORITY", defaultValue = "1")
    private int scaleUpRulePriority;
    @Inject
    @ConfigProperty(name = "SCALE_DOWN_RULE_PRIORITY", defaultValue = "2")
    private int scaleDownRulePriority;
    @Inject
    @ConfigProperty(name = "DISTRIBUTE_LOAD_RULE_PRIORITY", defaultValue = "3")
    private int distributeLoadRulePriority;
    private List<ScalingEventListener> eventListenerList = new CopyOnWriteArrayList<>();
    private JMXRulesEngine rulesEngine = new DefaultJMXRulesEngine(true);

    @Inject
    private Model model;

    public Model getModel(){
        return model;
    }
    public void process(){
        rulesEngine.fireRules();
    }


    public int getDistributeLoadRulePriority() {
        return distributeLoadRulePriority;
    }

    public void setDistributeLoadRulePriority(int distributeLoadRulePriority) {
        this.distributeLoadRulePriority = distributeLoadRulePriority;
    }

    public int getScaleDownRulePriority() {
        return scaleDownRulePriority;
    }

    public void setScaleDownRulePriority(int scaleDownRulePriority) {
        this.scaleDownRulePriority = scaleDownRulePriority;
    }


    public int getScaleUpRulePriority() {
        return scaleUpRulePriority;
    }

    public void setScaleUpRulePriority(int scaleUpRulePriority) {
        this.scaleUpRulePriority = scaleUpRulePriority;
    }


    public void add(ScalingEventListener scalingEventListener){
        eventListenerList.add(scalingEventListener);
    }

    public void remove(ScalingEventListener scalingEventListener){
        eventListenerList.remove(scalingEventListener);
    }

    protected void fireScalingUp(){
        for (ScalingEventListener scalingEventListener:eventListenerList){
            scalingEventListener.scaleUp();
        }
    }

    protected void fireScalingDown(){
        for (ScalingEventListener scalingEventListener:eventListenerList){
            scalingEventListener.scaleDown();
        }
    }

    protected void fireDistributeLoad(){
        for (ScalingEventListener scalingEventListener:eventListenerList){
            scalingEventListener.distributeLoad();
        }
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        rulesEngine.clearRules();
        eventListenerList.clear();
    }

    @Override
    protected void doStart() throws Exception {
        rulesEngine.registerJMXRule(new DefaultDistributeLoadRule(this,getDistributeLoadRulePriority()));
        rulesEngine.registerJMXRule(new DefaultScaleUpRule(this,getScaleUpRulePriority()));
        rulesEngine.registerJMXRule(new DefaultScaleDownRule(this,getScaleDownRulePriority()));
    }
}
