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

import io.fabric8.mq.controller.camel.CamelRouter;
import io.fabric8.mq.controller.model.BrokerControl;
import io.fabric8.mq.controller.model.Model;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;

import javax.inject.Inject;

/**
 * Holder of state for Brokers
 */

public class BrokerStateInfo extends ServiceSupport {
    @Inject
    protected BrokerControl brokerControl;
    @Inject
    protected Model model;
    @Inject
    protected AsyncExecutors asyncExecutors;
    @Inject
    protected MQControllerStatus controllerStatus;
    @Inject
    protected CamelRouter camelRouter;

    public AsyncExecutors getAsyncExectutors() {
        return asyncExecutors;
    }

    public BrokerControl getBrokerControl() {
        return brokerControl;
    }

    public void setBrokerControl(BrokerControl brokerControl) {
        this.brokerControl = brokerControl;
    }

    public MQControllerStatus getControllerStatus() {
        return controllerStatus;
    }

    public Model getModel() {
        return model;
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        serviceStopper.stop(brokerControl);
        serviceStopper.stop(model);
        serviceStopper.stop(asyncExecutors);
        serviceStopper.stop(camelRouter);
    }

    @Override
    protected void doStart() throws Exception {
        asyncExecutors.start();
        int numberOfCores = Runtime.getRuntime().availableProcessors();
        if (controllerStatus.getNumberOfSevers() < 1) {
            controllerStatus.setNumberOfSevers(numberOfCores);
        }
        if (controllerStatus.getNumberOfMultiplexers() < 1) {
            controllerStatus.setNumberOfMultiplexers(numberOfCores);
        }
        model.start();
        brokerControl.start();
        controllerStatus.setCamelRoutes(camelRouter.getCamelRoutes());
        camelRouter.start();
    }
}
