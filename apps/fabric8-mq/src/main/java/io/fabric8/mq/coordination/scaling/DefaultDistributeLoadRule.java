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

public class DefaultDistributeLoadRule extends ScalingRule {

    public DefaultDistributeLoadRule(ScalingEngine scalingEngine, int prioroity) {
        super(scalingEngine, "DistributeLoadRule", "Should we distribute load", prioroity);
    }

    @Override
    public boolean evaluateConditions() {
        called();
        boolean result = false;
        final BrokerModel leastLoaded = model.getLeastLoadedBroker();
        final BrokerModel mostLoaded = model.getMostLoadedBroker();

        if ((model.areBrokerLimitsExceeded(mostLoaded) || model.areDestinationLimitsExceeded(mostLoaded)) &&
                (!model.areBrokerLimitsExceeded(leastLoaded) && !model.areDestinationLimitsExceeded(leastLoaded))) {
            int toCopy = mostLoaded.getActiveDestinationCount() - leastLoaded.getActiveDestinationCount();
            if (toCopy > 0) {
                toCopy = toCopy / 2;
                if (toCopy > 0) {
                    result = true;
                }
            }
        }
        return result;
    }

    @Override
    public void performActions() throws Exception {
        executed();
        scalingEngine.fireDistributeLoad();
    }

}
