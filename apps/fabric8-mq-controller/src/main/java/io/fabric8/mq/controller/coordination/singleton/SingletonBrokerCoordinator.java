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

package io.fabric8.mq.controller.coordination.singleton;

import io.fabric8.mq.controller.coordination.BrokerChangeListener;
import io.fabric8.mq.controller.coordination.BrokerCoordinator;
import io.fabric8.mq.controller.coordination.BrokerView;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SingletonBrokerCoordinator implements BrokerCoordinator {
    private List<BrokerChangeListener> brokerChangeListenerList = new CopyOnWriteArrayList();

    @Override
    public void createBroker(BrokerView brokerView) {
        for (BrokerChangeListener brokerChangeListener : brokerChangeListenerList) {
            brokerChangeListener.created(brokerView);
        }
    }

    @Override
    public void updateBroker(BrokerView brokerView) {
        for (BrokerChangeListener brokerChangeListener : brokerChangeListenerList) {
            brokerChangeListener.updated(brokerView);
        }
    }

    @Override
    public void deleteBroker(BrokerView brokerView) {
        for (BrokerChangeListener brokerChangeListener : brokerChangeListenerList) {
            brokerChangeListener.deleted(brokerView.getBrokerName());
        }
    }

    @Override
    public void addBrokerChangeListener(BrokerChangeListener brokerChangeListener) {
        brokerChangeListenerList.add(brokerChangeListener);
    }

    @Override
    public void removeBrokerChangeListener(BrokerChangeListener brokerChangeListener) {
        brokerChangeListenerList.remove(brokerChangeListener);
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {

    }
}
