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

package io.fabric8.mq.controller;

import io.fabric8.mq.controller.coordination.BrokerControl;
import io.fabric8.mq.controller.util.MapTransportConnectionStateRegister;
import io.fabric8.mq.controller.util.TransportConnectionStateRegister;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;

/**
 * Holder of state for Brokers
 */
public class BrokerStateInfo extends ServiceSupport {
    private final MQController controller;
    private final BrokerControl brokerControl;
    private final TransportConnectionStateRegister transportConnectionStateRegister;

    BrokerStateInfo(MQController controller) {
        this.controller = controller;
        transportConnectionStateRegister = new MapTransportConnectionStateRegister();
        brokerControl = new BrokerControl(this);
    }

    public TransportConnectionStateRegister getTransportConnectionStateRegister() {
        return transportConnectionStateRegister;
    }

    public MQController getController() {
        return controller;
    }

    public BrokerControl getBrokerControl() {
        return brokerControl;
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        transportConnectionStateRegister.clear();
        serviceStopper.stop(brokerControl);
    }

    @Override
    protected void doStart() throws Exception {
        brokerControl.start();
    }
}
