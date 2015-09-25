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
package io.fabric8.mq;

import io.fabric8.mq.model.BrokerControl;
import io.fabric8.mq.util.WeldJUnitRunner;
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
public class MainTestMQTT {
    private static final Logger LOG = LoggerFactory.getLogger(MainTestMQTT.class);
    @Inject
    BrokerControl brokerControl;
    @Inject
    private Fabric8MQ fabric8MQ;

    @Before
    public void doStart() throws Exception {
        fabric8MQ.getFabric8MQStatus().setControllerPort(0);
        fabric8MQ.setBrokerControl(brokerControl);
        fabric8MQ.getFabric8MQStatus().setNumberOfMultiplexers(1);
        fabric8MQ.getFabric8MQStatus().setNumberOfSevers(1);
        fabric8MQ.start();
    }

    @After
    public void doStop() throws Exception {
        if (fabric8MQ != null) {
            fabric8MQ.stop();
        }
    }

    @Test
    public void test() throws Exception {
       /*
        MQTTClientPack openWirePack = new MQTTClientPack();
        openWirePack.setPort(fabric8MQ.getBoundPort());
        openWirePack.setNumberOfDestinations(1);
        openWirePack.setNumberOfProducers(1);
        openWirePack.setNumberOfConsumers(1);
        openWirePack.setNumberOfMessagesPerDestination(5000);
        openWirePack.start();
        openWirePack.doTheTest();
        openWirePack.stop();
        */

    }
}
