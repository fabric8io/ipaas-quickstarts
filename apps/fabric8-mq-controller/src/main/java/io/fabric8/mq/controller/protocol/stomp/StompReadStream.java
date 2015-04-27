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
package io.fabric8.mq.controller.protocol.stomp;

import org.apache.activemq.AsyncCallback;
import org.apache.activemq.command.Command;
import org.apache.activemq.util.DataByteArrayOutputStream;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.ReadStream;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

class StompReadStream extends ServiceSupport implements ReadStream<StompReadStream> {

    private final StompTransport transport;
    private final StompWireFormat wireFormat;
    private final DataByteArrayOutputStream dataOut;
    private final ReentrantLock lock;
    private Handler<Buffer> dataHandler;
    private boolean paused;
    private BlockingQueue<Send> queue;

    StompReadStream(StompTransport transport, final StompWireFormat wireFormat) {
        this.transport = transport;
        this.wireFormat = wireFormat;
        dataOut = new DataByteArrayOutputStream();
        lock = new ReentrantLock();
    }

    @Override
    public StompReadStream endHandler(Handler<Void> handler) {
        return this;
    }

    @Override
    public StompReadStream dataHandler(Handler<Buffer> handler) {
        dataHandler = handler;
        return this;
    }

    @Override
    public StompReadStream pause() {
        ReentrantLock lock = this.lock;
        try {
            lock.lockInterruptibly();
            paused = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
        return this;
    }

    @Override
    public StompReadStream resume() {
        final ReentrantLock lock = io.fabric8.mq.controller.protocol.stomp.StompReadStream.this.lock;
        try {
            lock.lockInterruptibly();
            if (paused) {
                final BlockingQueue<Send> queue = this.queue;
                if (queue != null && !queue.isEmpty()) {
                    Send send = queue.poll(50, TimeUnit.MILLISECONDS);
                    do {
                        Buffer buffer = io.fabric8.mq.controller.protocol.stomp.StompReadStream.this.createBuffer(send.getCommand());
                        if (buffer != null) {
                            final Handler<Buffer> dh = dataHandler;
                            if (dh != null) {
                                dh.handle(buffer);
                                send.onSuccess();
                            } else {
                                throw new IllegalStateException("No Data Handler");
                            }
                        }

                    } while (send != null && !queue.isEmpty());

                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            paused = false;
            lock.unlock();
        }
        return this;
    }

    @Override
    public StompReadStream exceptionHandler(Handler<Throwable> handler) {
        return this;
    }

    protected void sendToVertx(final Object o) throws IOException {
        sendToVertx(o, null);
    }

    protected void sendToVertx(final Object o, final AsyncCallback asyncCallback) throws IOException {
        final ReentrantLock lock = io.fabric8.mq.controller.protocol.stomp.StompReadStream.this.lock;
        try {
            lock.lockInterruptibly();
            if (paused) {
                if (queue == null) {
                    queue = new LinkedBlockingDeque<>();
                }
                queue.add(new Send((Command) o, asyncCallback));
            } else {
                Buffer buffer = io.fabric8.mq.controller.protocol.stomp.StompReadStream.this.createBuffer(o);
                if (buffer != null) {
                    final Handler<Buffer> dh = dataHandler;
                    if (dh != null) {
                        dh.handle(buffer);
                        if (asyncCallback != null) {
                            asyncCallback.onSuccess();
                        }
                    } else {
                        throw new IllegalStateException("No Data Handler");
                    }
                }

            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    private Buffer createBuffer(Object command) {
        Buffer buffer = null;

        if (command != null) {
            try {
                wireFormat.marshal(command, dataOut);
                dataOut.flush();
                int size = dataOut.size();
                byte[] data = new byte[size];
                System.arraycopy(dataOut.getData(), 0, data, 0, size);
                if (size > StompTransport.COMPACT_SIZE) {
                    dataOut.restart();
                }
                dataOut.reset();
                buffer = new Buffer(data);
            } catch (Throwable t) {
                transport.handleException(t);
            }
        }
        return buffer;
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        paused = false;
    }

    @Override
    protected void doStart() throws Exception {
    }

    private class Send {
        final private Command command;
        final private AsyncCallback callback;

        Send(Command command, AsyncCallback callback) {
            this.command = command;
            this.callback = callback;
        }

        Send(Command command) {
            this.command = command;
            this.callback = null;
        }

        Command getCommand() {
            return command;
        }

        void onSuccess() {
            if (callback != null) {
                callback.onSuccess();
            }
        }

        public String toString() {
            return "Send[" + command + "], callback = " + callback;
        }
    }
}
