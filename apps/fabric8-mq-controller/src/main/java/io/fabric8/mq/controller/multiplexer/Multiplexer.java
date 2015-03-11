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
import io.fabric8.mq.controller.sharding.MessageDistribution;
import org.apache.activemq.command.*;
import org.apache.activemq.transport.DefaultTransportListener;
import org.apache.activemq.transport.FutureResponse;
import org.apache.activemq.transport.ResponseCallback;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.util.IdGenerator;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Multiplexer extends ServiceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(Multiplexer.class);

    private final BrokerStateInfo brokerStateInfo;
    private final MessageDistribution messageDistribution;
    private final Map<Transport, MultiplexerInput> inputs = new ConcurrentHashMap<>();
    private final Map<ConsumerId, MultiplexerInput> consumerIdMultiplexerInputMap = new ConcurrentHashMap<>();
    private final IdGenerator inputId;
    private final ConnectionInfo multiplexerConnectionInfo;
    private final AtomicLong sessionIdGenerator = new AtomicLong();
    private final AtomicLong producerIdGenerator = new AtomicLong();
    private final AtomicLong consumerIdGenerator = new AtomicLong();
    private final AtomicLong transactionIdGenerator = new AtomicLong();
    private final String name;
    private String userName = "";
    private String password = "";

    public Multiplexer(String name, BrokerStateInfo brokerStateInfo, MessageDistribution messageDistribution) {
        this.name = name;
        this.brokerStateInfo = brokerStateInfo;
        this.messageDistribution = messageDistribution;
        inputId = new IdGenerator("InTransport");
        multiplexerConnectionInfo = new ConnectionInfo(new ConnectionId(inputId.generateSanitizedId()));
    }

    public int getInputSize() {
        return inputs.size();
    }

    public void onFailure(Throwable e) {
        if (isStopping() || isStopped()) {
            LOG.debug("Transport error: {}", e.getMessage(), e);
        } else {
            try {
                LOG.error("Transport error: {} ", e.getMessage(), e);
                stop();
            } catch (Throwable ex) {
            }
        }

    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public long getNextSessionId() {
        return sessionIdGenerator.incrementAndGet();
    }

    public long getNextProducerId() {
        return producerIdGenerator.incrementAndGet();
    }

    public long getNextConsumerId() {
        return consumerIdGenerator.incrementAndGet();
    }

    public long getNextTransactionId() {
        return transactionIdGenerator.incrementAndGet();
    }

    public void addInput(Transport transport) throws Exception {
        if (transport != null && !isStopping() && !isStopped()) {
            MultiplexerInput multiplexerInput = new MultiplexerInput(this, brokerStateInfo, multiplexerConnectionInfo.getConnectionId(), transport);
            inputs.put(transport, multiplexerInput);
            multiplexerInput.start();
        }
    }

    public void removeInput(MultiplexerInput multiplexerInput) {
        if (multiplexerInput != null) {
            if (inputs.remove(multiplexerInput.getInput()) != null) {

                List<ConsumerId> removeList = new ArrayList<>();
                for (Map.Entry<ConsumerId, MultiplexerInput> keyEntry : consumerIdMultiplexerInputMap.entrySet()) {
                    if (keyEntry.getValue().equals(multiplexerInput)) {
                        removeList.add(keyEntry.getKey());
                    }
                }
                for (ConsumerId consumerId : removeList) {
                    consumerIdMultiplexerInputMap.remove(consumerId);
                }
            }
        }
    }

    public void removeInput(Transport transport) {
        MultiplexerInput multiplexerInput = inputs.get(transport);
        removeInput(multiplexerInput);
    }

    public void sendOutAll(final MultiplexerInput input, final Command command) {
        if (command != null) {
            try {
                if (command.isResponseRequired()) {
                    final int commandId = command.getCommandId();

                    messageDistribution.asyncSendAll(command, new ResponseCallback() {
                        @Override
                        public void onCompletion(FutureResponse futureResponse) {
                            try {
                                Response response = futureResponse.getResult();
                                process(input, commandId, response);
                            } catch (Throwable e) {
                                e.printStackTrace();
                                onFailure(e);
                            }
                        }
                    });
                } else {
                    messageDistribution.sendAll(command);
                }
            } catch (Throwable e) {
                e.printStackTrace();
                onFailure(e);
            }
        }
    }

    public void sendOut(final MultiplexerInput input, ActiveMQDestination destination, Command command) {
        if (command != null) {
            try {
                if (command.isResponseRequired()) {
                    final int commandId = command.getCommandId();

                    messageDistribution.asyncSend(destination, command, new ResponseCallback() {
                        @Override
                        public void onCompletion(FutureResponse futureResponse) {
                            try {
                                Response response = futureResponse.getResult();
                                process(input, commandId, response);
                            } catch (Throwable e) {
                                onFailure(e);
                            }
                        }
                    });
                } else {
                    messageDistribution.send(destination, command);
                }
            } catch (Throwable e) {
                onFailure(e);
            }
        }
    }

    public void registerConsumer(ConsumerId consumerId, MultiplexerInput input) {
        consumerIdMultiplexerInputMap.put(consumerId, input);
    }

    public void unregisterConsumer(ConsumerId consumerId) {
        consumerIdMultiplexerInputMap.remove(consumerId);
    }

    @Override
    protected void doStart() throws Exception {

        messageDistribution.setTransportListener(new DefaultTransportListener() {
            @Override
            public void onCommand(Object o) {
                try {
                    processCommand(o);
                } catch (Throwable error) {
                    onFailure(error);
                }
            }

            @Override
            public void onException(IOException error) {
                if (!isStopping()) {
                    onFailure(error);
                }
            }
        });

        messageDistribution.start();
        multiplexerConnectionInfo.setClientId(getName());
        multiplexerConnectionInfo.setUserName(getUserName());
        multiplexerConnectionInfo.setPassword(getPassword());
        messageDistribution.sendAll(multiplexerConnectionInfo);

    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) {
        try {
            if (!messageDistribution.isStopped()) {
                messageDistribution.sendAll(new ShutdownInfo());
            }
            serviceStopper.stop(messageDistribution);
        } catch (Throwable e) {
        }
    }

    protected void processCommand(Object o) throws Exception {
        Command command = (Command) o;
        if (command.isResponse()) {
            LOG.error("Unexpected response " + command);
        } else if (command.isMessageDispatch()) {
            MessageDispatch messageDispatch = (MessageDispatch) command;
            ConsumerId consumerId = messageDispatch.getConsumerId();
            MultiplexerInput input = consumerIdMultiplexerInputMap.get(consumerId);
            if (input != null) {
                input.oneway(messageDispatch);
            } else {
                LOG.debug("Couldn't find MultiplexerInput for consumerId:" + consumerId);
            }
        } else if (command.isBrokerInfo() || command.isWireFormatInfo()) {
            //not a lot to do with this
        } else if (command.getClass() == ConnectionError.class) {
            ConnectionError ce = (ConnectionError) command;
            onFailure(ce.getException());
        } else {

            switch (command.getDataStructureType()) {
                case KeepAliveInfo.DATA_STRUCTURE_TYPE:
                case ShutdownInfo.DATA_STRUCTURE_TYPE:
                case ConnectionControl.DATA_STRUCTURE_TYPE:
                    break;
                default:
                    LOG.warn("Unexpected remote command: {}", command);
            }
        }

    }

    protected void doAsyncProcess(Runnable run) {
        brokerStateInfo.getController().execute(run);
    }

    protected void process(MultiplexerInput input, int realCorrelationId, Response response) throws IOException {

        if (response.isException()) {
            ExceptionResponse er = (ExceptionResponse) response;
            onFailure(er.getException());
        } else {
            Response copy = new Response();
            response.copy(copy);
            copy.setCorrelationId(realCorrelationId);
            input.oneway(copy);
        }

    }
}
