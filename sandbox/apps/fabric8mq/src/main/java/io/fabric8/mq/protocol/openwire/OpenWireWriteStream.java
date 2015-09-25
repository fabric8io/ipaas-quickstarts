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

package io.fabric8.mq.protocol.openwire;

import org.apache.activemq.openwire.OpenWireFormat;
import org.apache.activemq.util.DataByteArrayInputStream;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.WriteStream;

class OpenWireWriteStream extends ServiceSupport implements WriteStream<OpenWireWriteStream> {
    private final OpenWireTransport transport;
    private final OpenWireFormat wireFormat;
    private final DataByteArrayInputStream dataIn;
    private Handler<Void> drainHandler;
    private Buffer readBuffer = null;
    private int readStart;

    OpenWireWriteStream(OpenWireTransport transport, OpenWireFormat wireFormat) {
        this.transport = transport;
        this.wireFormat = wireFormat;
        this.dataIn = new DataByteArrayInputStream();
    }

    @Override
    public OpenWireWriteStream write(final Buffer buffer) {
        transport.runOnContext(new VoidHandler() {
            @Override
            protected void handle() {
                process(buffer);
            }
        });
        return this;
    }

    @Override
    public OpenWireWriteStream setWriteQueueMaxSize(int i) {
        return this;
    }

    @Override
    public boolean writeQueueFull() {
        return false;
    }

    @Override
    public OpenWireWriteStream drainHandler(Handler<Void> handler) {
        this.drainHandler = handler;
        transport.runOnContext(new VoidHandler() {
            public void handle() {
                callDrainHandler();
            }
        });
        return this;
    }

    @Override
    public OpenWireWriteStream exceptionHandler(Handler<Throwable> handler) {
        return this;
    }

    private void callDrainHandler() {
        if (drainHandler != null) {
            if (!writeQueueFull()) {
                try {
                    drainHandler.handle(null);
                } catch (Throwable t) {
                    transport.handleHandlerException(t);
                }
            }
        }
    }

    private void process(Buffer event) {
        if (!isStopped() && !isStopping()) {
            if (readBuffer == null) {
                readBuffer = event;
            } else {
                readBuffer.appendBuffer(event);
            }
            try {
                while (readBuffer != null && ((readStart + 4) < readBuffer.length())) {
                    int packetLength = readBuffer.getInt(readStart);
                    //add the length back in - cause OpenWire expects it
                    packetLength += 4;
                    int len = readStart + packetLength;
                    if (len <= readBuffer.length()) {
                        //do the read

                        Buffer buffer = readBuffer.getBuffer(readStart, (readStart + packetLength));
                        dataIn.restart(buffer.getBytes());
                        Object object = wireFormat.unmarshal(dataIn);
                        transport.doConsume(object);
                        readStart = len;
                    } else {
                        break;
                    }
                    /*
                     * GC: messages can come in distinct clumps - we might be able to throw away the buffer
                     * - else we will try to compact it
                     */
                    if (readStart >= readBuffer.length()) {
                        readBuffer = null;
                        readStart = 0;

                    } else if (readStart > OpenWireTransport.COMPACT_SIZE) {
                        readBuffer = readBuffer.getBuffer(readStart, readBuffer.length());
                        readStart = 0;
                    }
                }

            } catch (Throwable e) {
                transport.handleException(e);
            }
        }
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {

    }

    @Override
    protected void doStart() throws Exception {

    }
}
