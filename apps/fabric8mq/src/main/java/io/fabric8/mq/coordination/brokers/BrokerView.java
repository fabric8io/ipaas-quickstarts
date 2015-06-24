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

package io.fabric8.mq.coordination.brokers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.fabric8.mq.MessageDistribution;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.transport.DefaultTransportListener;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.transport.TransportFactory;
import org.apache.activemq.transport.TransportListener;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The BrokerView is used to hold the current state of a broker - typically populated
 * via discovery
 */
public class BrokerView extends ServiceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(BrokerView.class);
    private final List<String> destinations = new ArrayList<>();
    @JsonIgnore
    private final Map<MessageDistribution, Transport> transportMap = new ConcurrentHashMap<>();
    @JsonIgnore
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private String brokerName;
    private String brokerId;
    private String uri;
    private BrokerOverview brokerOverview;

    public String getBrokerId() {
        return brokerId;
    }

    public void setBrokerId(String brokerId) {
        this.brokerId = brokerId;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public List<String> getDestinations() {
        return destinations;
    }

    public Set<ActiveMQDestination> getQueues() {
        return brokerOverview.getQueueOverviews().keySet();
    }

    public BrokerOverview getBrokerOverview() {
        return brokerOverview;
    }

    public void setBrokerOverview(BrokerOverview brokerOverview) {
        this.brokerOverview = brokerOverview;
        if (brokerOverview != null) {
            synchronized (destinations) {
                destinations.clear();
                for (ActiveMQDestination destination : brokerOverview.getQueueOverviews().keySet()) {
                    destinations.add(destination.toString());
                }
                for (ActiveMQDestination destination : brokerOverview.getTopicOverviews().keySet()) {
                    destinations.add(destination.toString());
                }
            }
        }
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Transport getTransport(MessageDistribution key) {
        Transport result = null;
        readWriteLock.readLock().lock();
        try {
            result = transportMap.get(key);
        } finally {
            readWriteLock.readLock().unlock();
        }
        return result;
    }

    public void addTransport(MessageDistribution messageDistribution, Transport transport) {
        transportMap.put(messageDistribution, transport);
        messageDistribution.transportCreated(brokerId, transport);

    }

    public void removeTransport(MessageDistribution messageDistribution) {
        Transport transport = transportMap.remove(messageDistribution);
        if (transport != null) {
            if (transport.isConnected()) {
                try {
                    transport.stop();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void removeTransport(Transport transport) {
        readWriteLock.writeLock().lock();
        for (Map.Entry<MessageDistribution, Transport> entry : transportMap.entrySet()) {
            if (transport.equals(entry.getValue())) {
                transportMap.remove(entry.getKey());
            }
        }
    }

    public void createTransport(final MessageDistribution messageDistribution) throws Exception {
        URI location = new URI("failover:(" + uri + "?wireFormat.cacheEnabled=false)?maxReconnectAttempts=0");
        TransportFactory factory = TransportFactory.findTransportFactory(location);
        final Transport transport = factory.doConnect(location);
        transport.setTransportListener(new TransportListener() {
            private final TransportListener transportListener = messageDistribution.getTransportListener();

            public void onCommand(Object o) {
                transportListener.onCommand(o);
            }

            public void onException(IOException e) {
                removeTransport(transport);
                transportListener.onException(e);
            }

            public void transportInterupted() {
                transportListener.transportInterupted();
            }

            public void transportResumed() {
                transportListener.transportResumed();
            }
        });
        transport.start();
        addTransport(messageDistribution, transport);
        LOG.info("Created transport for " + getBrokerName() + " to " + getUri());
    }

    public Collection<MessageDistribution> detachTransport() {
        List<MessageDistribution> result = new ArrayList<>();
        for (MessageDistribution messageDistribution : transportMap.keySet()) {
            Transport transport = transportMap.remove(messageDistribution);
            if (transport != null) {
                if (transport.isConnected()) {
                    //prevent exceptions being perculated when we stop this thing
                    transport.setTransportListener(new DefaultTransportListener());
                    try {
                        transport.stop();
                    } catch (Throwable ignored) {
                    }
                }
            }
            result.add(messageDistribution);
        }
        return result;
    }

    public void attachTransport(Collection<MessageDistribution> messageDistributions) {
        for (MessageDistribution messageDistribution : messageDistributions) {
            try {
                createTransport(messageDistribution);
            } catch (Exception ignore) {
                //this will get attached at the next update
            }
        }
    }

    public void updateTransport() throws Exception {
        for (Map.Entry<MessageDistribution, Transport> entry : transportMap.entrySet()) {
            Transport transport = entry.getValue();
            if (transport == null || !transport.isConnected() || transport.isDisposed()) {
                createTransport(entry.getKey());
            }
        }
    }

    public void getReadLock() {
        readWriteLock.readLock().lock();
    }

    public void unlockReadLock() {
        try {
            readWriteLock.readLock().unlock();
        } catch (IllegalMonitorStateException ignored) {
        }
    }

    public void getWriteLock() {
        readWriteLock.writeLock().lock();
    }

    public int readLockCount() {
        return readWriteLock.getReadLockCount();

    }

    public void unlockWriteLock() {
        try {
            readWriteLock.writeLock().unlock();
        } catch (IllegalMonitorStateException ignored) {
        }
    }

    public void reset() {
        synchronized (destinations) {
            destinations.clear();
        }
    }

    public String toString() {
        return "BrokerView:" + brokerName + "[" + brokerId + "(" + getUri() + ")" + "]";
    }

    public int hashCode() {
        return brokerName.hashCode();
    }

    public boolean equals(Object other) {
        if (other instanceof BrokerView) {
            BrokerView brokerView = (BrokerView) other;
            return brokerName != null && brokerView.brokerName != null && brokerView.brokerName.equals(brokerName);
        }
        return false;
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        unlockWriteLock();
        unlockReadLock();
    }

    @Override
    protected void doStart() throws Exception {

    }
}
