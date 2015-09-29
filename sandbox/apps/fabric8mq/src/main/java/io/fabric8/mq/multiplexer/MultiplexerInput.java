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

import io.fabric8.mq.AsyncExecutors;
import io.fabric8.mq.model.DestinationStatisticsMBean;
import io.fabric8.mq.model.InboundConnection;
import io.fabric8.mq.model.Model;
import io.fabric8.mq.util.LRUCache;
import io.fabric8.mq.util.TransportConnectionStateRegister;
import org.apache.activemq.advisory.AdvisorySupport;
import org.apache.activemq.command.*;
import org.apache.activemq.state.CommandVisitor;
import org.apache.activemq.state.ConsumerState;
import org.apache.activemq.state.ProducerState;
import org.apache.activemq.transport.DefaultTransportListener;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.transport.TransportListener;
import org.apache.activemq.transport.TransportSupport;
import org.apache.activemq.util.ServiceStopper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class MultiplexerInput extends TransportSupport implements CommandVisitor, InboundConnection {
    private static Logger LOG = LoggerFactory.getLogger(MultiplexerInput.class);
    private final Multiplexer multiplexer;
    private final Model model;
    private final String protocol;
    private final String name;
    private final AsyncExecutors asyncExecutors;
    private final TransportConnectionStateRegister multiplexerConnectionStateRegister;
    private final ConnectionId multiplexerConnectionId;
    private final SessionId multiplexerSessionId;
    private final Transport input;
    final private Map<SessionId, SessionId> sessionIdMap = new ConcurrentHashMap<>();
    final private Map<ProducerId, ProducerId> producerIdMap = new ConcurrentHashMap<>();
    final private Map<ConsumerId, ConsumerId> originalConsumerIdKeyMap = new ConcurrentHashMap<>();
    final private Map<ConsumerId, ConsumerId> multiplexerConsumerIdKeyMap = new ConcurrentHashMap<>();
    final private Map<TransactionId, TransactionId> transactionIdMap = new LRUCache<>(10000);
    final private DestinationRegister destinationRegister;
    private ConnectionInfo connectionInfo;
    private AtomicLong inboundMessageCount = new AtomicLong();
    private AtomicLong outboundMessageCount = new AtomicLong();

    MultiplexerInput(Multiplexer multiplexer, String name, String protocol, AsyncExecutors asyncExecutors, TransportConnectionStateRegister transportConnectionStateRegister, Transport input) {
        this.multiplexer = multiplexer;
        this.name = name;
        this.protocol = protocol;
        this.model = multiplexer.getModel();
        this.asyncExecutors = asyncExecutors;
        this.multiplexerConnectionStateRegister = transportConnectionStateRegister;
        this.input = input;
        this.multiplexerConnectionId = multiplexer.getMultiplexerConnectionInfo().getConnectionId();
        this.multiplexerSessionId = multiplexer.getMultiplexerSessionInfo().getSessionId();
        this.destinationRegister = new DestinationRegister(model, this);

    }

    public Transport getInput() {
        return input;
    }

    public ConnectionId getMultiplexerConnectionId() {
        return multiplexerConnectionId;
    }

    public void setTransportListener(TransportListener commandListener) {
        super.setTransportListener(commandListener);
        input.setTransportListener(commandListener);
    }

    public Multiplexer getMultiplexer() {
        return multiplexer;
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        //clear down our ConnectionState
        for (SessionId sessionId : sessionIdMap.values()) {
            removeSession(sessionId);
        }

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
        model.remove(this);

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
        model.add(this);
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
        ActiveMQDestination destination = messageDispatch.getDestination();
        if (model.canDispatch(destination)) {
            ConsumerId consumerId = messageDispatch.getConsumerId();
            ConsumerId originalConsumerId = getOriginalConsumerId(consumerId);
            messageDispatch.setConsumerId(originalConsumerId);
            input.oneway(messageDispatch);
            destinationRegister.addMessageOutbound(messageDispatch.getDestination());
            outboundMessageCount.incrementAndGet();
            model.dispatched(this, destination);
        }
    }

    @Override
    public String getName() {
        return name;
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
        copy.setConnectionId(getMultiplexerConnectionId());
        /*
        multiplexer.sendOutAll(this, copy);
        multiplexerConnectionStateRegister.registerConnectionState(copy.getConnectionId(), copy);
        */
        Response response = new Response();
        response.setCorrelationId(connectionInfo.getCommandId());
        return response;
    }

    @Override
    public Response processAddSession(SessionInfo sessionInfo) throws Exception {
        SessionInfo copy = new SessionInfo();
        sessionInfo.copy(copy);
        SessionId originalSessionId = sessionInfo.getSessionId();
        SessionId sessionId = new SessionId(getMultiplexerConnectionId(), multiplexer.getNextSessionId());
        sessionIdMap.put(originalSessionId, sessionId);
        copy.setSessionId(sessionId);
        multiplexer.sendOutAll(this, copy);
        multiplexerConnectionStateRegister.addSession(copy);
        return null;
    }

    @Override
    public Response processAddProducer(ProducerInfo producerInfo) throws Exception {
        ProducerInfo copy = producerInfo.copy();
        SessionId originalSessionId = producerInfo.getProducerId().getParentId();
        SessionId newSessionId = sessionIdMap.get(originalSessionId);
        if (newSessionId == null) {
            newSessionId = new SessionId(getMultiplexerConnectionId(), multiplexer.getNextSessionId());
            sessionIdMap.put(originalSessionId, newSessionId);
        }
        ProducerId producerId = new ProducerId(newSessionId, multiplexer.getNextProducerId());
        copy.setProducerId(producerId);
        producerIdMap.put(producerInfo.getProducerId(), producerId);
        multiplexer.sendOutAll(this, copy);
        multiplexerConnectionStateRegister.addProducer(copy);
        destinationRegister.registerProducer(producerInfo.getDestination());
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
                newSessionId = multiplexerSessionId;
            } else {
                newSessionId = new SessionId(getMultiplexerConnectionId(), multiplexer.getNextSessionId());
            }
            sessionIdMap.put(originalSessionId, newSessionId);
        }
        ConsumerId multiplexConsumerId = new ConsumerId(newSessionId, multiplexer.getNextConsumerId());

        copy.setConsumerId(multiplexConsumerId);
        storeConsumerId(originalConsumerId, multiplexConsumerId);

        /**
         * ToDo: We will no longer need to do this once we have a better way of distributing load
         */
        //copy.setPrefetchSize(1);

        multiplexer.registerConsumer(multiplexConsumerId, this);
        multiplexer.sendOutAll(this, copy);
        multiplexerConnectionStateRegister.addConsumer(copy);
        if (!AdvisorySupport.isAdvisoryTopic(consumerInfo.getDestination())) {
            destinationRegister.registerConsumer(consumerInfo.getDestination());
        }
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
            /*
            multiplexerConnectionStateRegister.unregisterConnectionState(getMultiplexerConnectionId());

            if (!multiplexer.isStopping() && !multiplexer.isStopped()) {
                multiplexer.sendOutAll(this, removeCommand);
            }
            */
        }

        //clear down our ConnectionState
        for (SessionId sessionId : sessionIdMap.values()) {
            if (multiplexerConnectionStateRegister.removeSession(sessionId) != null) {
                removeSession(sessionId);
            }
        }
        asyncExecutors.execute(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(1000);
                    stop();
                } catch (Throwable e) {
                    LOG.warn("Caught an error trying to stop");
                }
            }
        });
        return new Response();
    }

    @Override
    public Response processRemoveSession(SessionId sessionId, long l) throws Exception {
        SessionId multiplexerSessionId = sessionIdMap.remove(sessionId);
        if (multiplexerSessionId != null) {
            RemoveInfo removeInfo = new RemoveInfo(multiplexerSessionId);
            removeInfo.setLastDeliveredSequenceId(l);
            multiplexerConnectionStateRegister.removeSession(multiplexerSessionId);
            multiplexer.sendOutAll(this, removeInfo);
        }
        return null;
    }

    @Override
    public Response processRemoveProducer(ProducerId producerId) throws Exception {
        ProducerId originalProducerId = producerIdMap.remove(producerId);
        if (originalProducerId != null) {
            RemoveInfo removeInfo = new RemoveInfo(originalProducerId);
            ProducerState state = multiplexerConnectionStateRegister.removeProducer(originalProducerId);
            multiplexer.sendOutAll(this, removeInfo);
            if (state != null && state.getInfo() != null) {
                destinationRegister.unregisterProducer(state.getInfo().getDestination());
            }
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
            ConsumerState state = multiplexerConnectionStateRegister.removeConsumer(multiplexerConsumerId);
            multiplexer.sendOutAll(this, removeInfo);
            if (state != null && state.getInfo() != null) {
                destinationRegister.unregisterConsumer(state.getInfo().getDestination());
            }
        }
        return null;
    }

    @Override
    public Response processAddDestination(DestinationInfo destinationInfo) throws Exception {
        DestinationInfo copy = destinationInfo.copy();
        copy.setConnectionId(getMultiplexerConnectionId());
        multiplexer.sendOut(this, destinationInfo.getDestination(), copy);
        return null;
    }

    @Override
    public Response processRemoveDestination(DestinationInfo destinationInfo) throws Exception {
        DestinationInfo copy = destinationInfo.copy();
        copy.setConnectionId(getMultiplexerConnectionId());
        multiplexer.sendOut(this, destinationInfo.getDestination(), copy);
        return null;
    }

    @Override
    public Response processRemoveSubscription(RemoveSubscriptionInfo removeSubscriptionInfo) throws Exception {
        removeSubscriptionInfo.setConnectionId(getMultiplexerConnectionId());
        multiplexer.sendOutAll(this, removeSubscriptionInfo);
        return null;
    }

    @Override
    public Response processMessage(Message message) throws Exception {

        ProducerId originalProducerId = message.getProducerId();
        ProducerId newProducerId = producerIdMap.get(originalProducerId);
        if (newProducerId != null) {
            ActiveMQDestination destination = message.getDestination();
            Message copy = message.copy();
            copy.setProducerId(newProducerId);
            copy.setTransactionId(getMultiplexTransactionId(message.getOriginalTransactionId()));
            multiplexer.sendOut(this, destination, copy);
            destinationRegister.addMessageInbound(destination);
            inboundMessageCount.incrementAndGet();
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
            if (model.canDispatch(messageAck.getDestination())) {
                copy.setConsumerId(multiplexerConsumerId);
                multiplexer.sendOut(this, copy.getDestination(), copy);
            }
        }
        model.acked(this, messageAck.getDestination());

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
                    result = new LocalTransactionId(getMultiplexerConnectionId(), multiplexerTransactionId);
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

    private void removeByMultiplexerId(ConsumerId multiplexerId) {
        ConsumerId originalId = multiplexerConsumerIdKeyMap.remove(multiplexerId);
        if (originalId != null) {
            originalConsumerIdKeyMap.remove(originalId);
        }
    }

    private ConsumerId getMultiplexConsumerId(ConsumerId original) {
        return originalConsumerIdKeyMap.get(original);
    }

    private ConsumerId getOriginalConsumerId(ConsumerId multiplex) {
        return multiplexerConsumerIdKeyMap.get(multiplex);
    }

    private void removeSession(SessionId multiplexerDefinedSessionId) {

        for (ConsumerId multiplexerConsumerId : multiplexerConsumerIdKeyMap.keySet()) {
            if (multiplexerConsumerId.getParentId().equals(multiplexerDefinedSessionId)) {
                removeConsumer(multiplexerConsumerId);
            }
        }
        for (Map.Entry<ProducerId, ProducerId> entry : producerIdMap.entrySet()) {
            if (entry.getValue().getParentId().equals(multiplexerDefinedSessionId)) {
                removeProducer(entry.getValue());
                producerIdMap.remove(entry.getKey());
            }
        }
        if (!multiplexerDefinedSessionId.equals(multiplexerSessionId)) {
            multiplexerConnectionStateRegister.removeSession(multiplexerDefinedSessionId);
            for (Map.Entry<SessionId, SessionId> entry : sessionIdMap.entrySet()) {
                if (entry.getValue().equals(multiplexerDefinedSessionId)) {
                    sessionIdMap.remove(entry.getKey());
                    break;
                }
            }
            RemoveInfo removeInfo = new RemoveInfo(multiplexerDefinedSessionId);
            removeInfo.setLastDeliveredSequenceId(0);
            multiplexer.sendOutAll(this, removeInfo);
        }
    }

    private void removeConsumer(ConsumerId multiplexerConsumerId) {
        if (multiplexerConsumerId != null) {
            removeByMultiplexerId(multiplexerConsumerId);
            if (multiplexerConnectionStateRegister.removeConsumer(multiplexerConsumerId) != null) {
                multiplexer.unregisterConsumer(multiplexerConsumerId);

                RemoveInfo removeInfo = new RemoveInfo(multiplexerConsumerId);
                removeInfo.setLastDeliveredSequenceId(0);
                multiplexer.sendOutAll(this, removeInfo);
            }
        }

    }

    private void removeProducer(ProducerId multiplexerProducerId) {

        if (multiplexerProducerId != null) {
            multiplexerConnectionStateRegister.removeProducer(multiplexerProducerId);
            RemoveInfo removeInfo = new RemoveInfo(multiplexerProducerId);
            multiplexer.sendOutAll(this, removeInfo);
        }
    }

    @Override
    public long getOutboundMessageCount() {
        return outboundMessageCount.get();
    }

    @Override
    public long getInboundMessageCount() {
        return inboundMessageCount.get();
    }

    @Override
    public String getUrl() {
        return input.getRemoteAddress();
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public List<DestinationStatisticsMBean> getDestinations() {
        return destinationRegister.getDestinations();
    }
}
