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

import io.fabric8.mq.MessageDistribution;
import io.fabric8.mq.coordination.brokers.BrokerModel;
import io.fabric8.mq.coordination.brokers.BrokerTransport;
import org.apache.activemq.Service;
import org.apache.activemq.command.ActiveMQDestination;

import java.util.Collection;

public interface BrokerControl extends Service {

    Collection<BrokerTransport> getTransports(MessageDistribution messageDistribution);

    BrokerTransport getTransport(MessageDistribution messageDistribution, ActiveMQDestination destination);

    void addMessageDistribution(MessageDistribution messageDistribution);

    void removeMessageDistribution(MessageDistribution messageDistribution);

    Collection<BrokerModel> getBrokerModels();

    void addBrokerModelChangedListener(BrokerModelChangedListener brokerModelChangedListener);

    void removeBrokerModelChangedListener(BrokerModelChangedListener brokerModelChangedListener);
}


