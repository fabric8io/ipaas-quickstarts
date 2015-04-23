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

import io.fabric8.mq.controller.coordination.brokers.BrokerModel;

public class DefaultScaleUpRule extends BaseScalingRule {

    public DefaultScaleUpRule(ScalingEngine scalingEngine, int priority) {
        super(scalingEngine, "ScaleUpRule", "scale up brokers", priority);
    }

    @Override
    public boolean evaluateConditions() {
        boolean result = false;
        for (BrokerModel brokerModel : model.getBrokers()) {
            if (model.areBrokerLimitsExceeded(brokerModel) || model.areDestinationLimitsExceeded(brokerModel)) {
                result = true;
                break;
            }
        }
        return result;
    }

    @Override
    public void performActions() throws Exception {
        scalingEngine.fireScalingUp();
    }
}
