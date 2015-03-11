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
package io.fabric8.mq.controller.util;

import org.apache.activemq.command.ConnectionId;
import org.apache.activemq.command.ConsumerId;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.command.ProducerId;
import org.apache.activemq.command.ProducerInfo;
import org.apache.activemq.command.SessionId;
import org.apache.activemq.command.SessionInfo;

import java.util.List;
import java.util.Map;

/**
 *
 */

public interface TransportConnectionStateRegister {

    TransportConnectionState registerConnectionState(ConnectionId connectionId,
                                                     org.apache.activemq.command.ConnectionInfo state);

    TransportConnectionState unregisterConnectionState(ConnectionId connectionId);

    List<TransportConnectionState> listConnectionStates();

    Map<ConnectionId, TransportConnectionState> mapStates();

    TransportConnectionState lookupConnectionState(String connectionId);

    TransportConnectionState lookupConnectionState(ConsumerId id);

    TransportConnectionState lookupConnectionState(ProducerId id);

    TransportConnectionState lookupConnectionState(SessionId id);

    TransportConnectionState lookupConnectionState(ConnectionId connectionId);

    void addSession(SessionInfo info);

    void removeSession(SessionId sessionId);

    void addProducer(ProducerInfo info);

    void removeProducer(ProducerId producerId);

    void addConsumer(ConsumerInfo info);

    void removeConsumer(ConsumerId consumerId);

    boolean isEmpty();

    boolean doesHandleMultipleConnectionStates();

    void clear();

}
