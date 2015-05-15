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

import io.fabric8.mq.model.DestinationStatistics;
import io.fabric8.mq.model.DestinationStatisticsMBean;
import io.fabric8.mq.model.Model;
import org.apache.activemq.command.ActiveMQDestination;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DestinationRegister {
    private final Model model;
    private final MultiplexerInput multiplexerInput;
    private final ConcurrentMap<ActiveMQDestination, DestinationStatisticsReference> map = new ConcurrentHashMap<>();
    private final Lock lock = new ReentrantLock();

    DestinationRegister(Model model, MultiplexerInput multiplexerInput) {
        this.model = model;
        this.multiplexerInput = multiplexerInput;
    }

    public DestinationStatistics registerConsumer(ActiveMQDestination destination) {
        DestinationStatistics destinationStatistics = register(destination);
        if (destinationStatistics != null) {
            destinationStatistics.addConsumer();
        }
        return destinationStatistics;
    }

    public DestinationStatistics registerProducer(ActiveMQDestination destination) {
        DestinationStatistics destinationStatistics = register(destination);
        if (destinationStatistics != null) {
            destinationStatistics.addProducer();
        }
        return destinationStatistics;
    }

    public void unregisterConsumer(ActiveMQDestination destination) {
        DestinationStatistics destinationStatistics = unregister(destination);
        if (destinationStatistics != null) {
            destinationStatistics.removeConsumer();
        }
    }

    public void unregisterProducer(ActiveMQDestination destination) {
        DestinationStatistics destinationStatistics = unregister(destination);
        if (destinationStatistics != null) {
            destinationStatistics.removeProducer();
        }
    }

    public List<DestinationStatisticsMBean> getDestinations() {
        List<DestinationStatisticsMBean> list = new ArrayList<>();
        for (DestinationStatisticsReference destinationStatisticsReference : map.values()) {
            DestinationStatisticsMBean destinationStatistics = destinationStatisticsReference.get();
            list.add(destinationStatistics);
        }
        return list;
    }

    public void clear() {
        map.clear();
    }

    public void addMessageInbound(ActiveMQDestination destination) {
        if (destination != null) {
            DestinationStatisticsReference destinationStatisticsReference = map.get(destination);
            if (destinationStatisticsReference != null) {
                DestinationStatistics destinationStatistics = destinationStatisticsReference.get();
                destinationStatistics.addInboundMessage();
            }
        }
    }

    public void addMessageOutbound(ActiveMQDestination destination) {
        if (destination != null) {
            DestinationStatisticsReference destinationStatisticsReference = map.get(destination);
            if (destinationStatisticsReference != null) {
                DestinationStatistics destinationStatistics = destinationStatisticsReference.get();
                destinationStatistics.addOutboundMessage();
            }
        }
    }

    private DestinationStatistics register(ActiveMQDestination destination) {
        DestinationStatistics destinationStatistics = null;
        if (destination != null) {
            lock.lock();
            try {
                DestinationStatisticsReference destinationStatisticsReference = map.get(destination);
                if (destinationStatisticsReference == null) {
                    destinationStatistics = new DestinationStatistics(multiplexerInput.getName(), destination);
                    destinationStatisticsReference = new DestinationStatisticsReference(destinationStatistics);
                    map.put(destination, destinationStatisticsReference);
                    model.register(multiplexerInput, destinationStatistics);
                } else {
                    destinationStatisticsReference.register();
                }
            } finally {
                lock.unlock();
            }
        }
        return destinationStatistics;
    }

    private DestinationStatistics unregister(ActiveMQDestination destination) {
        DestinationStatistics destinationStatisticsImpl = null;
        if (destination != null) {
            lock.lock();
            try {
                DestinationStatisticsReference destinationStatisticsReference = map.get(destination);
                if (destinationStatisticsReference != null) {
                    if (destinationStatisticsReference.unregister()) {
                        map.remove(destination);
                        model.unregister(multiplexerInput, destinationStatisticsReference.get());
                    } else {
                        destinationStatisticsImpl = destinationStatisticsReference.get();
                    }
                }
            } finally {
                lock.unlock();
            }
        }
        return destinationStatisticsImpl;
    }

    private class DestinationStatisticsReference {
        private final DestinationStatistics destinationStatistics;
        private final AtomicInteger count;

        DestinationStatisticsReference(DestinationStatistics destinationStatistics) {
            this.destinationStatistics = destinationStatistics;
            this.count = new AtomicInteger(1);
        }

        DestinationStatisticsMBean register() {
            count.incrementAndGet();
            return destinationStatistics;
        }

        DestinationStatistics get() {
            return destinationStatistics;
        }

        boolean unregister() {
            return count.decrementAndGet() <= 0;
        }
    }
}
