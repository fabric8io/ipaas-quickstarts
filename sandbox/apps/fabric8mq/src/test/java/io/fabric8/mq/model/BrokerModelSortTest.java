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

package io.fabric8.mq.model;

import io.fabric8.mq.coordination.brokers.BrokerDestinationOverview;
import io.fabric8.mq.coordination.brokers.BrokerModel;
import io.fabric8.mq.coordination.brokers.BrokerOverview;
import io.fabric8.mq.coordination.brokers.BrokerView;
import io.fabric8.mq.util.WeldJUnitRunner;
import org.apache.activemq.command.ActiveMQDestination;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

@RunWith(WeldJUnitRunner.class)
public class BrokerModelSortTest {

    private static final int NUMBER = 10;
    @Inject
    private Model model;

    @Before
    public void setUp() throws Exception {

        model.start();
        for (int i = 0; i < NUMBER; i++) {
            String name = "Broker:" + i;
            BrokerOverview brokerOverview = new BrokerOverview();
            //brokerOverview.setTotalConnections(10-i);
            ActiveMQDestination destination = ActiveMQDestination.createDestination("queue." + i, ActiveMQDestination.QUEUE_TYPE);
            BrokerDestinationOverview brokerDestinationOverview = new BrokerDestinationOverview(destination);
            //destinationOverview.setNumberOfConsumers(i);
            //destinationOverview.setNumberOfProducers(i);
            brokerDestinationOverview.setQueueDepth(NUMBER - i);
            brokerOverview.addDestinationStatistics(brokerDestinationOverview);
            BrokerView brokerView = new BrokerView();
            brokerView.setBrokerId(name);
            brokerView.setBrokerName(name);
            brokerView.setBrokerOverview(brokerOverview);
            BrokerModel brokerModel = new BrokerModel(null, brokerView, model);
            brokerModel.start();
            model.add(brokerModel);

        }
    }

    @Test
    public void sortTest() throws Exception {
        BrokerModel leastLoaded = model.getLeastLoadedBroker();
        Assert.assertNotNull(leastLoaded);
        BrokerModel nextLeastLoaded = model.getNextLeastLoadedBroker(leastLoaded);
        Assert.assertNotNull(nextLeastLoaded);
        BrokerModel mostLoaded = model.getMostLoadedBroker();
        Assert.assertNotNull(mostLoaded);

        Assert.assertTrue(model.getLoad(leastLoaded) < model.getLoad(nextLeastLoaded));
        Assert.assertTrue(model.getLoad(nextLeastLoaded) < model.getLoad(mostLoaded));
    }
}
