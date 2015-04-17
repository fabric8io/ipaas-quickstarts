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

package io.fabric8.mq.controller.protocol.mqtt;

import io.fabric8.mq.controller.MQController;
import io.fabric8.mq.controller.protocol.ProtocolTransport;
import io.fabric8.mq.controller.protocol.ProtocolTransportFactory;
import org.apache.activemq.transport.TransportFactory;
import org.apache.activemq.transport.TransportServer;
import org.apache.activemq.transport.mqtt.MQTTWireFormat;

import java.io.IOException;
import java.net.URI;

public class MQTTTransportFactory extends TransportFactory implements ProtocolTransportFactory {

    public ProtocolTransport connect(MQController controller, String name) throws IOException {
        MQTTWireFormat wireFormat = new MQTTWireFormat();
        //OpenWireFormat wireFormat = new OpenWireFormat(1);
        MQTTTransport transport = new MQTTTransport(controller, name, wireFormat);
        return transport;
    }

    @Override
    public TransportServer doBind(URI uri) throws IOException {
        return null;
    }
}
