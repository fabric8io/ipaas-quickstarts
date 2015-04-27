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

package io.fabric8.mq.controller.protocol.openwire;

import io.fabric8.mq.controller.AsyncExecutors;
import io.fabric8.mq.controller.protocol.ProtocolTransport;
import io.fabric8.mq.controller.protocol.ProtocolTransportFactory;
import org.apache.activemq.openwire.OpenWireFormat;
import org.apache.activemq.openwire.OpenWireFormatFactory;
import org.apache.activemq.transport.TransportFactory;
import org.apache.activemq.transport.TransportServer;
import org.apache.activemq.wireformat.WireFormatFactory;
import org.vertx.java.core.Vertx;

import java.io.IOException;
import java.net.URI;

public class OpenWireTransportFactory extends TransportFactory implements ProtocolTransportFactory {
    protected WireFormatFactory wireFormatFactory = new OpenWireFormatFactory();

    public ProtocolTransport connect(Vertx vertx, AsyncExecutors asyncExecutors, String name) throws IOException {
        OpenWireFormat wireFormat = (OpenWireFormat) wireFormatFactory.createWireFormat();
        //OpenWireFormat wireFormat = new OpenWireFormat(1);
        OpenWireTransport openWireTransport = new OpenWireTransport(vertx, asyncExecutors, name, wireFormat);
        return openWireTransport;
    }

    @Override
    public TransportServer doBind(URI uri) throws IOException {
        return null;
    }
}
