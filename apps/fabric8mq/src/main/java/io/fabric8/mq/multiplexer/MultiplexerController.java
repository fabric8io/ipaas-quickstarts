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

package io.fabric8.mq.multiplexer;

import io.fabric8.mq.AsyncExecutors;
import io.fabric8.mq.BrokerStateInfo;
import io.fabric8.mq.model.BrokerControl;
import io.fabric8.mq.model.Model;
import io.fabric8.mq.sharding.ShardedMessageDistribution;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiplexerController extends ServiceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(MultiplexerController.class);
    private final String name;
    private final BrokerStateInfo brokerStateInfo;
    private final ShardedMessageDistribution shardedMessageDistribution;
    private final Multiplexer multiplexer;

    public MultiplexerController(String name, BrokerStateInfo brokerStateInfo) {
        this.name = name;
        this.brokerStateInfo = brokerStateInfo;
        Model model = brokerStateInfo.getModel();
        BrokerControl brokerControl = brokerStateInfo.getBrokerControl();
        AsyncExecutors asyncExecutors = brokerStateInfo.getAsyncExectutors();

        shardedMessageDistribution = new ShardedMessageDistribution(brokerControl);
        multiplexer = new Multiplexer(model, getName() + ".multiplexer", asyncExecutors, shardedMessageDistribution);
    }

    public String getName() {
        return name;
    }

    public int getInputSize() {
        return multiplexer.getInputSize();
    }

    public synchronized void addTransport(String protocol, Transport inbound) throws Exception {
        multiplexer.addInput(protocol, inbound);
    }

    public synchronized void removeTransport(Transport inbound) {
        multiplexer.removeInput(inbound);
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        serviceStopper.stop(multiplexer);
    }

    @Override
    protected void doStart() throws Exception {
        multiplexer.start();
    }
}
