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

package io.fabric8.mq.controller.coordination.internalmodel;

import io.fabric8.mq.controller.coordination.brokermodel.DestinationStatistics;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.command.Message;
import org.apache.activemq.command.ProducerInfo;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class DestinationInfo {
    private final DestinationStatistics destinationStatistics = new DestinationStatistics();
    private final Set<ConsumerInfo> consumers = new CopyOnWriteArraySet<>();
    private final Set<ProducerInfo> producers = new CopyOnWriteArraySet<>();

    public void messageIn(Message message) {
        destinationStatistics.getMessages().increment();
    }

    public void addConsumer(ConsumerInfo consumerInfo) {
        consumers.add(consumerInfo);
        destinationStatistics.getConsumers().increment();
    }

    public void removeConsumer(ConsumerInfo consumerInfo) {
        if (consumers.remove(consumerInfo)) {
            destinationStatistics.getConsumers().decrement();
        }
    }

    public void addProducer(ProducerInfo producerInfo) {
        producers.add(producerInfo);
        destinationStatistics.getProducers().increment();
    }

    public void removeProducer(ProducerInfo producerInfo) {
        if (producers.remove(producerInfo)) {
            destinationStatistics.getProducers().decrement();
        }
    }

    public DestinationStatistics getDestinationStatistics() {
        return destinationStatistics;
    }
}
