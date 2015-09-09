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
package io.fabric8.mq.protocol.mqtt;

import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.util.LRUCache;
import org.fusesource.mqtt.codec.PUBLISH;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MQTTPacketIdGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(MQTTPacketIdGenerator.class);
    private static final NonZeroSequenceGenerator messageIdGenerator = new NonZeroSequenceGenerator();
    private final static Map<String, PacketIdMaps> clientIdMap = new ConcurrentHashMap<>();

    private MQTTPacketIdGenerator() {
    }

    public static void startClientSession(String clientId) {
        if (!clientIdMap.containsKey(clientId)) {
            clientIdMap.put(clientId, new PacketIdMaps());
        }
    }

    public static boolean stopClientSession(String clientId) {
        return clientIdMap.remove(clientId) != null;
    }

    public static short setPacketId(String clientId, MQTTSubscription subscription, ActiveMQMessage message, PUBLISH publish) {
        final PacketIdMaps idMaps = clientIdMap.get(clientId);
        if (idMaps == null) {
            // maybe its a cleansession=true client id, use session less message id
            final short id = messageIdGenerator.getNextSequenceId();
            publish.messageId(id);
            return id;
        } else {
            return idMaps.setPacketId(subscription, message, publish);
        }
    }

    public static void ackPacketId(String clientId, short packetId) {
        final PacketIdMaps idMaps = clientIdMap.get(clientId);
        if (idMaps != null) {
            idMaps.ackPacketId(packetId);
        }
    }

    public static short getNextSequenceId(String clientId) {
        final PacketIdMaps idMaps = clientIdMap.get(clientId);
        return idMaps != null ? idMaps.getNextSequenceId() : messageIdGenerator.getNextSequenceId();
    }

    private static class PacketIdMaps {

        final Map<String, Short> activemqToPacketIds = new LRUCache<>(MQTTProtocolConverter.DEFAULT_CACHE_SIZE);
        final Map<Short, String> packetIdsToActivemq = new LRUCache<>(MQTTProtocolConverter.DEFAULT_CACHE_SIZE);
        private final NonZeroSequenceGenerator messageIdGenerator = new NonZeroSequenceGenerator();

        short setPacketId(MQTTSubscription subscription, ActiveMQMessage message, PUBLISH publish) {
            // subscription key
            final String keyStr = subscription.getConsumerInfo().getDestination().getPhysicalName() + ':' + message.getJMSMessageID();
            Short packetId;
            synchronized (activemqToPacketIds) {
                packetId = activemqToPacketIds.get(keyStr);
                if (packetId == null) {
                    packetId = getNextSequenceId();
                    activemqToPacketIds.put(keyStr, packetId);
                    packetIdsToActivemq.put(packetId, keyStr);
                } else {
                    // mark publish as duplicate!
                    publish.dup(true);
                }
            }
            publish.messageId(packetId);
            return packetId;
        }

        void ackPacketId(short packetId) {
            synchronized (activemqToPacketIds) {
                final String subscriptionKey = packetIdsToActivemq.remove(packetId);
                if (subscriptionKey != null) {
                    activemqToPacketIds.remove(subscriptionKey);
                }
            }
        }

        short getNextSequenceId() {
            return messageIdGenerator.getNextSequenceId();
        }

    }

    private static class NonZeroSequenceGenerator {

        private short lastSequenceId;

        public synchronized short getNextSequenceId() {
            final short val = ++lastSequenceId;
            return val != 0 ? val : ++lastSequenceId;
        }

    }

}
