/**
 * Copyright 2005-2015 Red Hat, Inc.
 * <p/>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.mq.controller;

import io.fabric8.mq.controller.model.BrokerControl;
import io.fabric8.mq.controller.util.WeldJUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/*
 * Test the entire thing - but will use the BrokerControlTestImpl instead of KubernetesControl
 */
@RunWith(WeldJUnitRunner.class)
public class MainTest {
    private static final Logger LOG = LoggerFactory.getLogger(MainTest.class);
    @Inject
    BrokerControl brokerControl;
    @Inject
    private MQController controller;

    @Before
    public void doStart() throws Exception {
        controller.getControllerStatus().setControllerPort(0);
        controller.setBrokerControl(brokerControl);
        controller.getControllerStatus().setNumberOfMultiplexers(1);
        controller.getControllerStatus().setNumberOfSevers(1);
        controller.start();
    }

    @After
    public void doStop() throws Exception {
        if (controller != null) {
            controller.stop();
        }
    }

    @Test
    public void test() throws Exception {
        /*
        OpenWireClientPack openWirePack = new OpenWireClientPack();
        openWirePack.setPort(controller.getBoundPort());
        openWirePack.setNumberOfDestinations(10);
        openWirePack.setNumberOfProducers(2);
        openWirePack.setNumberOfConsumers(2);
        openWirePack.setNumberOfMessagesPerDestination(10);
        openWirePack.start();
        openWirePack.doTheTest();
        openWirePack.stop();
        */
    }
}
