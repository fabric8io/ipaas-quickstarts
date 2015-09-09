/*
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

package io.fabric8.mq.protocol.stomp;

import org.apache.activemq.transport.stomp.StompFrame;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.WriteStream;

class StompWriteStream extends ServiceSupport implements WriteStream<StompWriteStream> {
    private final StompTransport transport;
    private Handler<Void> drainHandler;
    private StompVertxCodec codec;

    StompWriteStream(StompTransport transport, StompWireFormat wireFormat) {
        this.transport = transport;
        this.codec = new StompVertxCodec(this, wireFormat);
    }

    @Override
    public StompWriteStream write(final Buffer buffer) {
        transport.runOnContext(new VoidHandler() {
            @Override
            protected void handle() {
                process(buffer);
            }
        });
        return this;
    }

    @Override
    public StompWriteStream setWriteQueueMaxSize(int i) {
        return this;
    }

    @Override
    public boolean writeQueueFull() {
        return false;
    }

    @Override
    public StompWriteStream drainHandler(Handler<Void> handler) {
        this.drainHandler = handler;
        transport.runOnContext(new VoidHandler() {
            public void handle() {
                callDrainHandler();
            }
        });
        return this;
    }

    @Override
    public StompWriteStream exceptionHandler(Handler<Throwable> handler) {
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
            try {
                int length = event.length();
                org.fusesource.hawtbuf.DataByteArrayInputStream dataIn = new org.fusesource.hawtbuf.DataByteArrayInputStream(event.getBytes());
                codec.parse(dataIn, length);
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

    protected void consume(StompFrame frame) {
        transport.doConsumeStomp(frame);
    }
}
