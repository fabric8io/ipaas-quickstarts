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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;

import java.util.HashMap;
import java.util.Map;

public class DestinationStatistics extends ServiceSupport implements DestinationStatisticsMBean, MetricSet {
    private final ActiveMQDestination activeMQDestination;
    private final String name;
    private final Map<String, Metric> map = new HashMap<>();
    private final Counter consumers = new Counter();
    private final Counter producers = new Counter();
    private final Meter inboundMessages = new Meter();
    private final Meter outboundMessages = new Meter();

    public DestinationStatistics(String title, ActiveMQDestination activeMQDestination) {
        this.activeMQDestination = activeMQDestination;
        String str = title;
        str += "." + activeMQDestination.getDestinationTypeAsString() + "." + activeMQDestination.getPhysicalName();
        name = str;
        map.put(name + ".consumers", consumers);
        map.put(name + ".producers", producers);
        map.put(name + ".inBoundMessages", inboundMessages);
        map.put(name + ".outBoundMessages", outboundMessages);
    }

    @Override
    public String getName() {
        return name;
    }

    public void addInboundMessage() {
        inboundMessages.mark();
    }

    public void addOutboundMessage() {
        outboundMessages.mark();
    }

    public void addProducer() {
        producers.inc();
    }

    public void removeProducer() {
        producers.dec();
    }

    public void addConsumer() {
        consumers.inc();
    }

    public void removeConsumer() {
        consumers.dec();
    }

    @Override
    public String getActiveMQDestination() {
        if (activeMQDestination.isComposite()) {
            String name = activeMQDestination.toString();
            name = name.replaceAll(",", "&");
            return name;
        }
        return activeMQDestination.toString();
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return map;
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        for (String key : map.keySet()) {
            Model.METRIC_REGISTRY.remove(key);
        }
    }

    @Override
    protected void doStart() throws Exception {
        Model.METRIC_REGISTRY.registerAll(this);
    }
}
