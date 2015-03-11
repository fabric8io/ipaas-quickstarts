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

package io.fabric8.mq.controller.multiplexer;

import io.fabric8.mq.controller.BrokerStateInfo;
import org.apache.activemq.command.*;
import org.apache.activemq.state.CommandVisitor;
import org.apache.activemq.transport.DefaultTransportListener;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.transport.TransportListener;
import org.apache.activemq.transport.TransportSupport;
import org.apache.activemq.util.LRUCache;
import org.apache.activemq.util.ServiceStopper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class MultiplexerInput extends TransportSupport implements CommandVisitor {
    private static Logger LOG = LoggerFactory.getLogger(MultiplexerInput.class);
    private ConnectionInfo connectionInfo;
    private Map<SessionId, SessionId> sessionIdMap = new ConcurrentHashMap<>();
    private Map<ProducerId, ProducerId> producerIdMap = new ConcurrentHashMap<>();
    private Map<ConsumerId, ConsumerId> originalConsumerIdKeyMap = new ConcurrentHashMap<>();
    private Map<ConsumerId, ConsumerId> multiplexerConsumerIdKeyMap = new ConcurrentHashMap<>();
    private Map<TransactionId, TransactionId> transactionIdMap = new LRUCache<>(10000);
    private final Multiplexer multiplexer;
    private final BrokerStateInfo brokerStateInfo;
    private final ConnectionId multiplexerId;
    private final Transport input;

    MultiplexerInput(Multiplexer multiplexer, BrokerStateInfo brokerStateInfo, ConnectionId connectionId, Transport input) {
        this.multiplexer = multiplexer;
        this.brokerStateInfo = brokerStateInfo;
        this.input = input;
        this.multiplexerId = connectionId;
    }

    public Transport getInput() {
        return input;
    }

    public ConnectionId getMultiplexerId() {
        return multiplexerId;
    }

    public void setTransportListener(TransportListener commandListener) {
        super.setTransportListener(commandListener);
        input.setTransportListener(commandListener);
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        multiplexer.removeInput(this);
        if (serviceStopper != null) {
            serviceStopper.stop(input);
        } else {
            input.stop();
        }
        originalConsumerIdKeyMap.clear();
        multiplexerConsumerIdKeyMap.clear();
        sessionIdMap.clear();
        producerIdMap.clear();
        transactionIdMap.clear();

    }

    @Override
    protected void doStart() throws Exception {
        setTransportListener(new DefaultTransportListener() {
            @Override
            public void onCommand(Object command) {
                if (command.getClass() == ShutdownInfo.class) {
                    try {
                        stop();
                    } catch (Exception e) {
                        LOG.debug("Caught exception stopping", e);
                    }
                }

                try {
                    processCommand(command);
                } catch (Throwable error) {
                    onFailure(error);
                }
            }

            @Override
            public void onException(IOException error) {
                onFailure(error);
            }
        });

        this.input.start();
    }

    protected void processCommand(Object o) {
        try {
            Command command = (Command) o;
            Response response = command.visit(this);
            if (response != null) {
                //we are processing this locally - not via the Broker
                response.setCorrelationId(command.getCommandId());
                oneway(response);
            }

        } catch (Throwable e) {
            onFailure(e);
        }
    }

    @Override
    public void oneway(Object o) throws IOException {
        input.oneway(o);
    }

    public void oneway(MessageDispatch messageDispatch) throws IOException {
        ConsumerId consumerId = messageDispatch.getConsumerId();
        ConsumerId originalConsumerId = getOriginalConsumerId(consumerId);
        messageDispatch.setConsumerId(originalConsumerId);
        input.oneway(messageDispatch);
    }

    @Override
    public String getRemoteAddress() {
        return input.getRemoteAddress();
    }

    @Override
    public int getReceiveCounter() {
        return input.getReceiveCounter();
    }

    public void onFailure(Throwable e) {
        if (!isStopping()) {
            LOG.debug("Transport error: {}", e.getMessage(), e);
            try {
                stop();
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public Response processAddConnection(ConnectionInfo connectionInfo) throws Exception {
        this.connectionInfo = connectionInfo;
        ConnectionInfo copy = connectionInfo.copy();
        copy.setConnectionId(getMultiplexerId());
        brokerStateInfo.getTransportConnectionStateRegister().registerConnectionState(copy.getConnectionId(), copy);
        multiplexer.sendOutAll(this, connectionInfo);
        Response response = new Response();
        response.setCorrelationId(connectionInfo.getCommandId());
        input.oneway(response);
        return null;
    }

    @Override
    public Response processAddSession(SessionInfo sessionInfo) throws Exception {
        SessionInfo copy = new SessionInfo();
        sessionInfo.copy(copy);
        SessionId originalSessionId = sessionInfo.getSessionId();
        SessionId sessionId = new SessionId(getMultiplexerId(), multiplexer.getNextSessionId());
        sessionIdMap.put(originalSessionId, sessionId);
        copy.setSessionId(sessionId);
        brokerStateInfo.getTransportConnectionStateRegister().addSession(copy);
        multiplexer.sendOutAll(this, copy);
        return null;
    }

    @Override
    public Response processAddProducer(ProducerInfo producerInfo) throws Exception {
        ProducerInfo copy = producerInfo.copy();
        SessionId originalSessionId = producerInfo.getProducerId().getParentId();
        SessionId newSessionId = sessionIdMap.get(originalSessionId);
        if (newSessionId == null) {
            newSessionId = new SessionId(getMultiplexerId(), multiplexer.getNextSessionId());
            sessionIdMap.put(originalSessionId, newSessionId);
        }
        ProducerId producerId = new ProducerId(newSessionId, multiplexer.getNextProducerId());
        copy.setProducerId(producerId);
        producerIdMap.put(producerInfo.getProducerId(), producerId);
        brokerStateInfo.getTransportConnectionStateRegister().addProducer(copy);
        multiplexer.sendOutAll(this, copy);
        return null;
    }

    @Override
    public Response processAddConsumer(ConsumerInfo consumerInfo) throws Exception {
        ConsumerId originalConsumerId = consumerInfo.getConsumerId();
        ConsumerInfo copy = new ConsumerInfo();
        consumerInfo.copy(copy);
        SessionId originalSessionId = consumerInfo.getConsumerId().getParentId();
        SessionId newSessionId = sessionIdMap.get(originalSessionId);
        if (newSessionId == null) {
            //Connection Advisory Consumer sets session id to -1
            if (originalSessionId.getValue() == -1) {
                newSessionId = new SessionId(getMultiplexerId(), -1);
            } else {
                newSessionId = new SessionId(getMultiplexerId(), multiplexer.getNextSessionId());
            }
            sessionIdMap.put(originalSessionId, newSessionId);
        }
        ConsumerId multiplexConsumerId = new ConsumerId(newSessionId, multiplexer.getNextConsumerId());

        copy.setConsumerId(multiplexConsumerId);
        storeConsumerId(originalConsumerId, multiplexConsumerId);
        multiplexer.registerConsumer(multiplexConsumerId, this);
        brokerStateInfo.getTransportConnectionStateRegister().addConsumer(copy);
        multiplexer.sendOutAll(this, copy);
        return null;
    }

    @Override
    public Response processRemoveConnection(ConnectionId connectionId, long l) throws Exception {
        /**
         * The connection is shutting down - and will expect a response.
         * We don't forward these - as the remote Broker will get confused
         * so we sent back a response ourseleves
         */
        if (connectionInfo != null) {
            RemoveInfo removeCommand = connectionInfo.createRemoveCommand();
            removeCommand.setResponseRequired(false);
            removeCommand.setLastDeliveredSequenceId(l);
            brokerStateInfo.getTransportConnectionStateRegister().unregisterConnectionState(getMultiplexerId());
            if (!multiplexer.isStopping() && !multiplexer.isStopped()) {
                multiplexer.sendOutAll(this, removeCommand);
            }
        }
        return new Response();
    }

    @Override
    public Response processRemoveSession(SessionId sessionId, long l) throws Exception {
        SessionId originalSessionId = sessionIdMap.remove(sessionId);
        if (originalSessionId != null) {
            RemoveInfo removeInfo = new RemoveInfo(originalSessionId);
            removeInfo.setLastDeliveredSequenceId(l);
            brokerStateInfo.getTransportConnectionStateRegister().removeSession(originalSessionId);
            multiplexer.sendOutAll(this, removeInfo);
        }
        return null;
    }

    @Override
    public Response processRemoveProducer(ProducerId producerId) throws Exception {
        ProducerId originalProducerId = producerIdMap.remove(producerId);
        if (originalProducerId != null) {
            RemoveInfo removeInfo = new RemoveInfo(originalProducerId);
            brokerStateInfo.getTransportConnectionStateRegister().removeProducer(originalProducerId);
            multiplexer.sendOutAll(this, removeInfo);
        }
        return null;
    }

    @Override
    public Response processRemoveConsumer(ConsumerId consumerId, long l) throws Exception {
        ConsumerId multiplexerConsumerId = removeByOriginal(consumerId);
        multiplexer.unregisterConsumer(multiplexerConsumerId);
        if (multiplexerConsumerId != null) {
            RemoveInfo removeInfo = new RemoveInfo(multiplexerConsumerId);
            removeInfo.setLastDeliveredSequenceId(l);
            brokerStateInfo.getTransportConnectionStateRegister().removeConsumer(multiplexerConsumerId);
            multiplexer.sendOutAll(this, removeInfo);
        }
        return null;
    }

    @Override
    public Response processAddDestination(DestinationInfo destinationInfo) throws Exception {
        DestinationInfo copy = destinationInfo.copy();
        copy.setConnectionId(getMultiplexerId());
        multiplexer.sendOut(this, destinationInfo.getDestination(), copy);
        return null;
    }

    @Override
    public Response processRemoveDestination(DestinationInfo destinationInfo) throws Exception {
        DestinationInfo copy = destinationInfo.copy();
        copy.setConnectionId(getMultiplexerId());
        multiplexer.sendOut(this, destinationInfo.getDestination(), copy);
        return null;
    }

    @Override
    public Response processRemoveSubscription(RemoveSubscriptionInfo removeSubscriptionInfo) throws Exception {
        removeSubscriptionInfo.setConnectionId(getMultiplexerId());
        multiplexer.sendOutAll(this, removeSubscriptionInfo);
        return null;
    }

    @Override
    public Response processMessage(Message message) throws Exception {

        ProducerId originalProducerId = message.getProducerId();
        ProducerId newProducerId = producerIdMap.get(originalProducerId);
        if (newProducerId != null) {
            Message copy = message.copy();
            copy.setProducerId(newProducerId);
            copy.setTransactionId(getMultiplexTransactionId(message.getOriginalTransactionId()));
            multiplexer.sendOut(this, copy.getDestination(), copy);
        } else {
            LOG.error("Cannot find producerId for " + originalProducerId);
        }
        return null;
    }

    @Override
    public Response processMessageAck(MessageAck messageAck) throws Exception {
        MessageAck copy = new MessageAck();
        messageAck.copy(copy);
        copy.setMessageCount(messageAck.getMessageCount());
        copy.setTransactionId(getMultiplexTransactionId(messageAck.getTransactionId()));
        ConsumerId consumerId = messageAck.getConsumerId();
        ConsumerId multiplexerConsumerId = getMultiplexConsumerId(consumerId);
        if (multiplexerConsumerId != null) {
            copy.setConsumerId(multiplexerConsumerId);
            multiplexer.sendOut(this, copy.getDestination(), copy);
        }

        return null;
    }

    @Override
    public Response processMessagePull(MessagePull messagePull) throws Exception {
        ConsumerId consumerId = messagePull.getConsumerId();
        ConsumerId multiplexerConsumerId = getMultiplexConsumerId(consumerId);
        if (multiplexerConsumerId != null) {
            messagePull.setConsumerId(multiplexerConsumerId);
            multiplexer.sendOut(this, messagePull.getDestination(), messagePull);
        }
        return null;
    }

    @Override
    public Response processBeginTransaction(TransactionInfo transactionInfo) throws Exception {
        transactionInfo.setTransactionId(getMultiplexTransactionId(transactionInfo.getTransactionId()));
        multiplexer.sendOutAll(this, transactionInfo);
        return null;
    }

    @Override
    public Response processPrepareTransaction(TransactionInfo transactionInfo) throws Exception {
        transactionInfo.setTransactionId(getMultiplexTransactionId(transactionInfo.getTransactionId()));
        multiplexer.sendOutAll(this, transactionInfo);
        return null;
    }

    @Override
    public Response processCommitTransactionOnePhase(TransactionInfo transactionInfo) throws Exception {
        transactionInfo.setTransactionId(getMultiplexTransactionId(transactionInfo.getTransactionId()));
        multiplexer.sendOutAll(this, transactionInfo);
        return null;
    }

    @Override
    public Response processCommitTransactionTwoPhase(TransactionInfo transactionInfo) throws Exception {
        transactionInfo.setTransactionId(getMultiplexTransactionId(transactionInfo.getTransactionId()));
        multiplexer.sendOutAll(this, transactionInfo);
        return null;
    }

    @Override
    public Response processRollbackTransaction(TransactionInfo transactionInfo) throws Exception {
        transactionInfo.setTransactionId(getMultiplexTransactionId(transactionInfo.getTransactionId()));
        multiplexer.sendOutAll(this, transactionInfo);
        return null;
    }

    @Override
    public Response processWireFormat(WireFormatInfo wireFormatInfo) throws Exception {
        return null;
    }

    @Override
    public Response processKeepAlive(KeepAliveInfo keepAliveInfo) throws Exception {
        return null;
    }

    @Override
    public Response processShutdown(ShutdownInfo shutdownInfo) throws Exception {
        multiplexer.doAsyncProcess(new Runnable() {
            @Override
            public void run() {
                try {
                    stop();
                } catch (Exception e) {
                    LOG.debug("Caught exception stopping", e);
                }
            }
        });
        return null;
    }

    @Override
    public Response processFlush(FlushCommand flushCommand) throws Exception {
        multiplexer.sendOutAll(this, flushCommand);
        return null;
    }

    @Override
    public Response processBrokerInfo(BrokerInfo brokerInfo) throws Exception {
        multiplexer.sendOutAll(this, brokerInfo);
        return null;
    }

    @Override
    public Response processRecoverTransactions(TransactionInfo transactionInfo) throws Exception {
        transactionInfo.setTransactionId(getMultiplexTransactionId(transactionInfo.getTransactionId()));
        multiplexer.sendOutAll(this, transactionInfo);
        return null;
    }

    @Override
    public Response processForgetTransaction(TransactionInfo transactionInfo) throws Exception {
        transactionInfo.setTransactionId(getAndForgetMultiplexTransactionId(transactionInfo.getTransactionId()));
        multiplexer.sendOutAll(this, transactionInfo);
        return null;
    }

    @Override
    public Response processEndTransaction(TransactionInfo transactionInfo) throws Exception {
        transactionInfo.setTransactionId(getAndForgetMultiplexTransactionId(transactionInfo.getTransactionId()));
        multiplexer.sendOutAll(this, transactionInfo);
        return null;
    }

    @Override
    public Response processMessageDispatchNotification(MessageDispatchNotification messageDispatchNotification) throws Exception {
        multiplexer.sendOutAll(this, messageDispatchNotification);
        return null;
    }

    @Override
    public Response processProducerAck(ProducerAck producerAck) throws Exception {
        multiplexer.sendOutAll(this, producerAck);
        return null;
    }

    @Override
    public Response processMessageDispatch(MessageDispatch messageDispatch) throws Exception {
        multiplexer.sendOutAll(this, messageDispatch);
        return null;
    }

    @Override
    public Response processControlCommand(ControlCommand controlCommand) throws Exception {
        return null;
    }

    @Override
    public Response processConnectionError(ConnectionError connectionError) throws Exception {
        multiplexer.sendOutAll(this, connectionError);
        return null;
    }

    @Override
    public Response processConnectionControl(ConnectionControl connectionControl) throws Exception {
        multiplexer.sendOutAll(this, connectionControl);
        return null;
    }

    @Override
    public Response processConsumerControl(ConsumerControl consumerControl) throws Exception {
        multiplexer.sendOutAll(this, consumerControl);
        return null;
    }

    private TransactionId getMultiplexTransactionId(TransactionId originalId) {
        TransactionId result = originalId;
        if (originalId != null && originalId.isLocalTransaction()) {
            synchronized (transactionIdMap) {
                result = transactionIdMap.get(originalId);
                if (result == null) {
                    long multiplexerTransactionId = multiplexer.getNextTransactionId();
                    result = new LocalTransactionId(getMultiplexerId(), multiplexerTransactionId);
                    transactionIdMap.put(originalId, result);
                }
            }
        }
        return result;
    }

    private TransactionId getAndForgetMultiplexTransactionId(TransactionId originalId) {
        TransactionId result = originalId;
        if (originalId != null && originalId.isLocalTransaction()) {
            synchronized (transactionIdMap) {
                TransactionId value = transactionIdMap.remove(originalId);
                if (value != null) {
                    result = value;
                }
            }
        }
        return result;
    }

    private void storeConsumerId(ConsumerId original, ConsumerId multiplexer) {
        originalConsumerIdKeyMap.put(original, multiplexer);
        multiplexerConsumerIdKeyMap.put(multiplexer, original);
    }

    private ConsumerId removeByOriginal(ConsumerId original) {
        ConsumerId multiplexerId = originalConsumerIdKeyMap.get(original);
        if (multiplexerId != null) {
            multiplexerConsumerIdKeyMap.remove(multiplexerId);
        }
        return multiplexerId;
    }

    private ConsumerId getMultiplexConsumerId(ConsumerId original) {
        return originalConsumerIdKeyMap.get(original);
    }

    private ConsumerId getOriginalConsumerId(ConsumerId multiplex) {
        return multiplexerConsumerIdKeyMap.get(multiplex);
    }

}
