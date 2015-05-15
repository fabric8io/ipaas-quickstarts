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
package io.fabric8.mq.protocol.http;

import io.fabric8.mq.protocol.ProtocolDetector;
import io.fabric8.mq.util.ConnectionParameters;
import io.fabric8.mq.util.SocketWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;

/**
 */
public class HttpProtocol implements ProtocolDetector {
    private static final transient Logger LOG = LoggerFactory.getLogger(HttpProtocol.class);
    private static final String[] SCHEMES = new String[]{"http"};

    @Override
    public String getProtocolName() {
        return "http";
    }

    @Override
    public String[] getProtocolSchemes() {
        return SCHEMES;
    }

    public int getMaxIdentificationLength() {
        return "CONNECT".length();
    }

    @Override
    public boolean matches(Buffer buffer) {
        String header = buffer.toString();
        return
            header.startsWith("GET") ||
                header.startsWith("HEAD") ||
                header.startsWith("POST") ||
                header.startsWith("PUT") ||
                header.startsWith("DELETE") ||
                header.startsWith("OPTIONS") ||
                header.startsWith("TRACE") ||
                header.startsWith("CONNECT");
    }

    @Override
    public void snoopConnectionParameters(final SocketWrapper socket, Buffer received, final Handler<ConnectionParameters> handler) {
        handler.handle(new ConnectionParameters());
    }

}
