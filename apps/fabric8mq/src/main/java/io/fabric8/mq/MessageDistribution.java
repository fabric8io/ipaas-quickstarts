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

import io.fabric8.mq.util.TransportConnectionStateRegister;
import org.apache.activemq.Service;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.Command;
import org.apache.activemq.transport.ResponseCallback;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.transport.TransportListener;

import java.io.IOException;

public interface MessageDistribution extends Service {
    void sendAll(Command command) throws IOException;

    void sendAll(Command command, boolean force) throws IOException;

    void send(ActiveMQDestination destination, Command command) throws IOException;

    void asyncSendAll(Command command, ResponseCallback callback) throws IOException;

    void asyncSend(ActiveMQDestination destination, Command command, ResponseCallback callback) throws IOException;

    TransportListener getTransportListener();

    void setTransportListener(TransportListener transportListener);

    void transportCreated(String brokerId, Transport transport);

    void transportDestroyed(String brokerId);

    int getCurrentConnectedBrokerCount();

    boolean isStopped();

    TransportConnectionStateRegister getTransportConnectionStateRegister();

}
