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

import io.fabric8.mq.protocol.ProtocolTransport;
import org.vertx.java.core.streams.Pump;

public class ConnectedSocketInfo {

    private ConnectionParameters params;
    private ProtocolMapping protocolMapping;
    private SocketWrapper from;
    private ProtocolTransport to;
    private Pump readPump;
    private Pump writePump;

    public ConnectedSocketInfo() {
    }

    public Pump getWritePump() {
        return writePump;
    }

    public void setWritePump(Pump writePump) {
        this.writePump = writePump;
    }

    public ConnectionParameters getParams() {
        return params;
    }

    public void setParams(ConnectionParameters params) {
        this.params = params;
    }

    public ProtocolMapping getProtocolMapping() {
        return protocolMapping;
    }

    public void setProtocolMapping(ProtocolMapping protocolMapping) {
        this.protocolMapping = protocolMapping;
    }

    public SocketWrapper getFrom() {
        return from;
    }

    public void setFrom(SocketWrapper from) {
        this.from = from;
    }

    public ProtocolTransport getTo() {
        return to;
    }

    public void setTo(ProtocolTransport to) {
        this.to = to;
    }

    public Pump getReadPump() {
        return readPump;
    }

    public void setReadPump(Pump readPump) {
        this.readPump = readPump;
    }

    public void close() throws Exception {
        if (from != null) {
            from.close();
        }
        if (to != null) {
            to.stop();
        }
        if (readPump != null) {
            readPump.stop();
        }
        if (writePump != null) {
            writePump.stop();
        }

    }
}
