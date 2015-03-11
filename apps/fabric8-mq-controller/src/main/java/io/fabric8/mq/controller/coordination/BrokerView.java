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

package io.fabric8.mq.controller.coordination;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.fabric8.mq.controller.sharding.MessageDistribution;
import org.apache.activemq.transport.Transport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BrokerView {
    private String brokerName;
    private String brokerId;
    private String uri;
    private final List<String> destinations = new ArrayList<>();
    @JsonIgnore
    private final Map<MessageDistribution, Transport> transportMap = new ConcurrentHashMap<>();
    @JsonIgnore
    private long timeStamp;
    @JsonIgnore
    private boolean gcCandidate;

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

    public void setDestinations(List<String> destinations) {
        synchronized (this.destinations) {
            this.destinations.clear();
            this.destinations.addAll(destinations);
        }
    }

    public void addDestination(String name) {
        destinations.add(name);
        gcCandidate = false;
    }

    public void removeDestination(String name) {
        destinations.remove(name);
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void updateDestinations(BrokerView other) {
        if (other != this && other.getBrokerName().equals(getBrokerName())) {
            synchronized (destinations) {
                destinations.clear();
                destinations.addAll(other.destinations);
            }
        }
    }

    public Transport getTransport(MessageDistribution key) {
        return transportMap.get(key);
    }

    public void addTransport(MessageDistribution messageDistribution, Transport transport) {
        transportMap.put(messageDistribution, transport);
    }

    public void removeTransport(MessageDistribution messageDistribution) {
        Transport transport = transportMap.remove(messageDistribution);
        if (transport != null) {
            if (transport.isConnected()) {
                try {
                    transport.stop();
                } catch (Exception e) {
                }
            }
        }
    }

    public void markSweep() {
        if (destinations.isEmpty()) {
            if (!gcCandidate) {
                timeStamp = System.currentTimeMillis();
                gcCandidate = true;
            }
        }
    }

    @JsonIgnore
    public boolean isGC() {
        return (destinations.isEmpty() && gcCandidate && (System.currentTimeMillis() > (timeStamp + BrokerControl.GC_TIME)));
    }

    public void reset() {
        synchronized (destinations) {
            destinations.clear();
        }
    }

    public String toString() {
        String str = "BrokerView[" + brokerName + "(" + getUri() + ")" + "]";
        return str;
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
}
