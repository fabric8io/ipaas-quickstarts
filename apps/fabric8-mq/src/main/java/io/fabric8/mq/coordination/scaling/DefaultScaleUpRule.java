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

public class DefaultScaleUpRule extends ScalingRule {

    public DefaultScaleUpRule(ScalingEngine scalingEngine, int priority) {
        super(scalingEngine, "ScaleUpRule", "scale up brokers", priority);
    }

    @Override
    public boolean evaluateConditions() {
        called();
        boolean result = false;
        if (!model.isMaximumNumberOfBrokersReached()) {
            for (BrokerModel brokerModel : model.getBrokers()) {
                if (model.areBrokerConnectionLimitsExceeded(brokerModel)) {
                    if (!isSpareConnectionsOnExistingBrokers(1)) {
                        result = true;
                        break;
                    }
                }
            }
            if (!result) {
                for (BrokerModel brokerModel : model.getBrokers()) {
                    if (model.areDestinationLimitsExceeded(brokerModel)) {
                        if (!isSpareDestinationsOnExistingBrokers(1)) {
                            result = true;
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void performActions() throws Exception {
        executed();
        scalingEngine.fireScalingUp();
    }

    private boolean isSpareDestinationsOnExistingBrokers(int number) {
        int spareDestinations = 0;
        for (BrokerModel brokerModel : model.getBrokers()) {
            spareDestinations += model.spareDestinations(brokerModel);
        }
        return spareDestinations - number > 0;
    }

    private boolean isSpareConnectionsOnExistingBrokers(int number) {
        int spareConnections = 0;
        for (BrokerModel brokerModel : model.getBrokers()) {
            spareConnections += model.spareConnections(brokerModel);
        }
        return spareConnections - number > 0;
    }
}
