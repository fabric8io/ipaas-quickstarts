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

import io.fabric8.mq.coordination.brokers.BrokerModel;

public class DefaultScaleDownRule extends ScalingRule implements ScaleDownRuleMBean {

    private int scaleDownThreshold = 50;

    public DefaultScaleDownRule(ScalingEngine scalingEngine, int priority) {
        super(scalingEngine, "ScaleDownRule", "scale down brokers if total load below threshold", priority);
    }

    @Override
    public boolean evaluateConditions() {
        called();
        boolean result = false;
        if (model.getBrokerCount() > 1) {
            int load = 0;
            for (BrokerModel brokerModel : model.getBrokers()) {
                load += model.getLoad(brokerModel);
            }
            if (load == 0 || (load * 100) / model.getBrokerCount() < 50) {
                result = true;
            }
        }
        return result;
    }

    @Override
    public void performActions() throws Exception {
        executed();
        scalingEngine.fireScalingDown();
    }

    @Override
    public int getScaleDownLoadThreshold() {
        return scaleDownThreshold;
    }

    @Override
    public void setScaleDownLoadThreshold(int load) {
        scaleDownThreshold = load;
    }
}
