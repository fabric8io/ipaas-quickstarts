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
package io.fabric8.mq.util;

import org.apache.activemq.command.ConnectionId;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.activemq.command.ConsumerId;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.command.ProducerId;
import org.apache.activemq.command.ProducerInfo;
import org.apache.activemq.command.SessionId;
import org.apache.activemq.command.SessionInfo;
import org.apache.activemq.state.ConsumerState;
import org.apache.activemq.state.ProducerState;
import org.apache.activemq.state.SessionState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */

public class TransportConnectionStateRegister {

    private Map<ConnectionId, TransportConnectionState> connectionStates = new ConcurrentHashMap<>();

    public TransportConnectionState registerConnectionState(ConnectionId connectionId, ConnectionInfo info) {
        TransportConnectionState state = new TransportConnectionState(info);
        return connectionStates.put(connectionId, state);
    }

    public TransportConnectionState unregisterConnectionState(ConnectionId connectionId) {
        TransportConnectionState rc = connectionStates.remove(connectionId);
        if (rc != null && rc.getReferenceCounter() != null && rc.getReferenceCounter().get() > 1) {
            rc.decrementReference();
            connectionStates.put(connectionId, rc);
        }
        return rc;
    }

    public List<TransportConnectionState> listConnectionStates() {

        List<TransportConnectionState> rc = new ArrayList<TransportConnectionState>();
        rc.addAll(connectionStates.values());
        return rc;
    }

    public TransportConnectionState lookupConnectionState(String connectionId) {
        return connectionStates.get(new ConnectionId(connectionId));
    }

    public TransportConnectionState lookupConnectionState(ConsumerId id) {
        return lookupConnectionState(id.getConnectionId());
    }

    public TransportConnectionState lookupConnectionState(ProducerId id) {
        return lookupConnectionState(id.getConnectionId());
    }

    public TransportConnectionState lookupConnectionState(SessionId id) {
        return lookupConnectionState(id.getConnectionId());
    }

    public TransportConnectionState lookupConnectionState(ConnectionId connectionId) {
        return connectionStates.get(connectionId);
    }

    public void addSession(SessionInfo info) {
        if (info != null && info.getSessionId() != null && info.getSessionId().getConnectionId() != null) {
            TransportConnectionState cs = lookupConnectionState(info.getSessionId().getConnectionId());
            if (cs != null) {
                cs.addSession(info);
            }
        }
    }

    public SessionState removeSession(SessionId sessionId) {
        if (sessionId != null && sessionId.getConnectionId() != null) {
            TransportConnectionState cs = lookupConnectionState(sessionId.getConnectionId());
            if (cs != null) {
                return cs.removeSession(sessionId);
            }
        }
        return null;
    }

    public void addProducer(ProducerInfo info) {
        if (info != null && info.getProducerId() != null && info.getProducerId().getParentId() != null && info.getProducerId().getConnectionId() != null) {
            TransportConnectionState cs = lookupConnectionState(info.getProducerId().getConnectionId());
            if (cs != null) {
                SessionState sessionState = cs.getSessionState(info.getProducerId().getParentId());
                if (sessionState != null) {
                    sessionState.addProducer(info);
                }
            }
        }
    }

    public ProducerState removeProducer(ProducerId producerId) {
        ProducerState result = null;
        if (producerId != null && producerId.getConnectionId() != null && producerId.getParentId() != null) {
            TransportConnectionState cs = lookupConnectionState(producerId.getConnectionId());
            if (cs != null) {
                SessionState sessionState = cs.getSessionState(producerId.getParentId());
                if (sessionState != null) {
                    result = sessionState.removeProducer(producerId);
                }
            }
        }
        return result;
    }

    public void addConsumer(ConsumerInfo info) {
        if (info != null && info.getConsumerId() != null && info.getConsumerId().getParentId() != null && info.getConsumerId().getConnectionId() != null) {
            TransportConnectionState cs = lookupConnectionState(info.getConsumerId().getConnectionId());
            if (cs != null) {
                SessionState sessionState = cs.getSessionState(info.getConsumerId().getParentId());
                if (sessionState != null) {
                    sessionState.addConsumer(info);
                }
            }
        }
    }

    public ConsumerState removeConsumer(ConsumerId consumerId) {
        if (consumerId != null && consumerId.getConnectionId() != null && consumerId.getParentId() != null) {
            TransportConnectionState cs = lookupConnectionState(consumerId.getConnectionId());
            if (cs != null) {
                SessionState sessionState = cs.getSessionState(consumerId.getParentId());
                if (sessionState != null) {
                    return sessionState.removeConsumer(consumerId);
                }
            }

        }
        return null;
    }

    public boolean doesHandleMultipleConnectionStates() {
        return true;
    }

    public boolean isEmpty() {
        return connectionStates.isEmpty();
    }

    public void clear() {
        connectionStates.clear();

    }

    public Map<ConnectionId, TransportConnectionState> mapStates() {
        return new HashMap<ConnectionId, TransportConnectionState>(connectionStates);
    }

}
