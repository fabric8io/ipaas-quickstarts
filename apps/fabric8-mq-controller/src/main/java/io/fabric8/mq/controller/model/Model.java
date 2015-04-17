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

package io.fabric8.mq.controller.model;

import com.codahale.metrics.MetricRegistry;
import io.fabric8.mq.controller.coordination.brokers.BrokerModel;
import io.fabric8.mq.controller.multiplexer.Multiplexer;
import io.fabric8.mq.controller.multiplexer.MultiplexerInput;
import org.apache.activemq.Service;
import org.apache.activemq.command.ActiveMQDestination;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface Model extends Service {

    String DEFAULT_JMX_DOMAIN = "io.fabric8.mq";
    MetricRegistry METRIC_REGISTRY = new MetricRegistry();

    BrokerLimitsConfig getBrokerLimitsConfig();

    void add(Multiplexer multiplexer);

    void remove(Multiplexer multiplexer);

    void add(MultiplexerInput multiplexer);

    void remove(MultiplexerInput multiplexer);

    void add(BrokerModel brokerModel);

    void remove(BrokerModel brokerModel);

    void register(MultiplexerInput multiplexer, DestinationStatistics destinationStatistics);

    void unregister(MultiplexerInput multiplexer, DestinationStatistics destinationStatistics);

    boolean areBrokerLimitsExceeded(BrokerModel brokerModel);

    boolean areDestinationLimitsExceeded(BrokerModel brokerModel);

    boolean canScaleDownBrokers();

    boolean shouldScaleUpBrokers();

    BrokerModel getMostLoadedBroker();

    BrokerModel getNextLeastLoadedBroker(BrokerModel brokerModel);

    BrokerModel getLeastLoadedBroker();

    int getLoad(BrokerModel brokerModel);

    BrokerModel getBrokerById(String id);

    Collection<BrokerModel> getBrokers();

    List<ActiveMQDestination> getSortedDestinations(BrokerModel brokerModel, int maxNumber);

    List<ActiveMQDestination> getSortedDestinations(BrokerModel brokerModel);

    Set<ActiveMQDestination> getActiveDestinations(BrokerModel brokerModel);

    int getBrokerCount();
}
