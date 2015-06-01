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

import io.fabric8.mq.coordination.brokers.BrokerModel;
import io.fabric8.mq.model.BrokerControl;
import io.fabric8.mq.model.BrokerModelChangedListener;
import io.fabric8.mq.model.Model;
import io.fabric8.mq.multiplexer.Multiplexer;
import io.fabric8.mq.sharding.ShardedMessageDistribution;
import io.fabric8.mq.util.TestConnection;
import io.fabric8.mq.util.TestTransportServer;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.transport.TransportAcceptListener;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;

import javax.inject.Inject;
import java.util.Collection;

public class TestController extends ServiceSupport {
    private static String LOCATION = "vm://test";
    @Inject
    private Model model;
    @Inject
    private BrokerControl brokerControl;
    private MessageDistribution messageDistribution;
    private TestTransportServer transportServer;
    @Inject
    private AsyncExecutors asyncExecutors;
    private Multiplexer multiplexer;

    public TestController() {

    }

    public Model getModel() {
        return model;
    }

    public Transport connect() throws Exception {
        return transportServer.connect();
    }

    public TestConnection createTestConnection(String clientId) throws Exception {
        return transportServer.createTestConnection(clientId);
    }

    public Collection<BrokerModel> getBrokerModels() {
        return brokerControl.getBrokerModels();
    }

    public void addBrokerModelChangedListener(BrokerModelChangedListener brokerModelChangedListener) {
        brokerControl.addBrokerModelChangedListener(brokerModelChangedListener);
    }

    public void removeBrokerModelChangedListener(BrokerModelChangedListener brokerModelChangedListener) {
        brokerControl.removeBrokerModelChangedListener(brokerModelChangedListener);
    }

    protected void doStart() throws Exception {

        messageDistribution = new ShardedMessageDistribution(brokerControl);
        multiplexer = new Multiplexer(model, "multiplexer.test", asyncExecutors, messageDistribution);
        model.start();
        transportServer = new TestTransportServer(LOCATION);

        asyncExecutors.start();
        brokerControl.start();
        messageDistribution.start();
        multiplexer.start();
        transportServer.setTransportAcceptListener((new TransportAcceptListener() {
            @Override
            public void onAccept(Transport transport) {
                try {
                    multiplexer.addInput("openwire", transport);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onAcceptError(Exception e) {
                e.printStackTrace();
            }
        }));
        transportServer.start();

    }

    protected void doStop(ServiceStopper stopper) throws Exception {
        stopper.stop(asyncExecutors);
        stopper.stop(multiplexer);
        stopper.stop(multiplexer);
        stopper.stop(messageDistribution);
        stopper.stop(transportServer);
    }

}
