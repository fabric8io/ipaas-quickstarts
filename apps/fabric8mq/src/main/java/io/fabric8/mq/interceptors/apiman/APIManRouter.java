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

package io.fabric8.mq.interceptors.apiman;

import io.fabric8.mq.interceptors.MessageInterceptorRegistry;
import io.fabric8.mq.interceptors.MessageRouter;
import org.apache.activemq.command.Command;
import org.apache.activemq.command.Message;
import org.apache.activemq.command.MessageDispatch;
import org.apache.activemq.command.Response;
import org.apache.activemq.state.CommandVisitorAdapter;
import org.apache.activemq.transport.DefaultTransportListener;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.transport.TransportSupport;
import org.apache.activemq.util.ServiceStopper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class APIManRouter extends TransportSupport implements MessageRouter {
    public static final String REGISTRY_NAME = "apiman";
    private static final Logger LOG = LoggerFactory.getLogger(APIManRouter.class);
    private final Response RESPONSE = new Response();
    private Transport input;

    public APIManRouter(Transport input) {
        this.input = input;

    }

    public void process(Command command) {
        if (command != null) {
            try {
                Response response = command.visit(new CommandVisitorAdapter() {
                    public Response processMessage(Message send) throws Exception {
                        if (MessageInterceptorRegistry.getInstance(APIManRouter.REGISTRY_NAME).processCommand(APIManRouter.this, send)) {
                            return RESPONSE;
                        }
                        return null;
                    }

                    public Response processMessageDispatch(MessageDispatch dispatch) throws Exception {
                        if (MessageInterceptorRegistry.getInstance(APIManRouter.REGISTRY_NAME).processCommand(APIManRouter.this, dispatch)) {
                            return RESPONSE;
                        }
                        return null;
                    }

                });
                if (response == null) {
                    doConsume(command);
                }
            } catch (Throwable e) {
                LOG.error("Unable to process " + command, e);
            }

        }

    }

    @Override
    public String getRemoteAddress() {
        return input.getRemoteAddress();
    }

    @Override
    public int getReceiveCounter() {
        return input.getReceiveCounter();
    }

    public void oneway(Object message) throws IOException {
        input.oneway(message);
    }

    public void inject(Command command) throws Exception {
        doConsume(command);
    }

    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        input.stop();
    }

    @Override
    protected void doStart() throws Exception {
        input.setTransportListener(new DefaultTransportListener() {
            @Override
            public void onCommand(Object command) {
                try {
                    process((Command) command);
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

    public void onFailure(Throwable e) {
        if (!isStopping()) {
            LOG.debug("Transport error: {}", e.getMessage(), e);
            try {
                stop();
            } catch (Exception ignore) {
            }
        }
    }
}
