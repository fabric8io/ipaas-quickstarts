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

import org.apache.activemq.transport.MutexTransport;
import org.apache.activemq.transport.ResponseCorrelator;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.transport.TransportAcceptListener;
import org.apache.activemq.transport.vm.VMTransportServer;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;

import java.net.URI;

public class TestTransportServer extends ServiceSupport implements TransportAcceptListener {
    private final String location;
    private VMTransportServer vmTransportServer;
    private TransportAcceptListener transportAcceptListener;
    private URI uri;

    public TestTransportServer(String location) {
        this.location = location;
    }

    public URI getUri() {
        return uri;
    }

    public TransportAcceptListener getTransportAcceptListener() {
        return transportAcceptListener;
    }

    public void setTransportAcceptListener(TransportAcceptListener transportAcceptListener) {
        this.transportAcceptListener = transportAcceptListener;
    }

    public Transport connect() throws Exception {
        if (isStarted()) {
            Transport transport = vmTransportServer.connect();
            transport = new MutexTransport(transport);
            transport = new ResponseCorrelator(transport);
            return transport;
        }
        return null;
    }

    public TestConnection createTestConnection(String clientId) throws Exception {
        Transport transport = connect();
        TestConnection connection = new TestConnection(transport);
        connection.setClientID(clientId);
        connection.start();
        return connection;
    }

    @Override
    public void onAccept(Transport transport) {
        TransportAcceptListener t = getTransportAcceptListener();
        if (t != null) {
            t.onAccept(transport);
        } else {
            System.err.println("No TransportAcceptListener is set");
        }
    }

    @Override
    public void onAcceptError(Exception e) {
        TransportAcceptListener t = getTransportAcceptListener();
        if (t != null) {
            t.onAcceptError(e);
        } else {
            System.err.println("No TransportAcceptListener is set");
        }
    }

    @Override
    protected void doStart() throws Exception {
        uri = new URI(location);
        vmTransportServer = new VMTransportServer(uri, false);
        vmTransportServer.setAcceptListener(this);
        vmTransportServer.start();

    }

    @Override
    protected void doStop(ServiceStopper stopper) throws Exception {
        stopper.stop(vmTransportServer);
    }
}
