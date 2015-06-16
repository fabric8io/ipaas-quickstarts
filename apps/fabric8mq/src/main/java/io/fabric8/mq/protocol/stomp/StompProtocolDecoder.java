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
package io.fabric8.mq.protocol.stomp;

import io.fabric8.mq.protocol.ProtocolDecoder;
import io.fabric8.mq.util.BufferSupport;
import org.apache.activemq.transport.stomp.Stomp;
import org.apache.activemq.transport.stomp.StompFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implements protocol decoding for the STOMP protocol.
 */
class StompProtocolDecoder extends ProtocolDecoder<StompFrame> {

    private static final transient Logger LOG = LoggerFactory.getLogger(StompProtocolDecoder.class);

    private final StompProtocol protocol;
    public boolean trim = false;
    final Action<StompFrame> read_action = new Action<StompFrame>() {
        public StompFrame apply() throws IOException {
            Buffer line = readUntil((byte) '\n', StompProtocol.maxCommandLength, "The maximum command length was exceeded");
            if (line != null) {
                Buffer action = BufferSupport.chomp(line);
                if (trim) {
                    action = BufferSupport.trim(action);
                }
                if (action.length() > 0) {
                    StompFrame frame = new StompFrame(action.toString());
                    nextDecodeAction = read_headers(frame);
                    return nextDecodeAction.apply();
                }
            }
            return null;
        }
    };

    public StompProtocolDecoder(StompProtocol protocol) {
        this.protocol = protocol;
    }

    @Override
    protected Action<StompFrame> initialDecodeAction() {
        return read_action;
    }

    private Action<StompFrame> read_headers(final StompFrame frame) {

        final Map<String, String> headers = new HashMap<>();
        final AtomicInteger contentLength = new AtomicInteger(-1);
        return new Action<StompFrame>() {
            public StompFrame apply() throws IOException {
                Buffer line = readUntil((byte) '\n', protocol.maxHeaderLength, "The maximum header length was exceeded");
                while (line != null) {
                    line = BufferSupport.chomp(line);
                    if (line.length() > 0) {

                        if (protocol.maxHeaders != -1 && headers.size() > protocol.maxHeaders) {
                            throw new IOException("The maximum number of headers was exceeded");
                        }

                        try {
                            int seperatorIndex = BufferSupport.indexOf(line, Stomp.COLON);
                            if (seperatorIndex < 0) {
                                throw new IOException("Header line missing separator [" + line.toString() + "]");
                            }
                            Buffer name = line.getBuffer(0, seperatorIndex);
                            if (trim) {
                                name = BufferSupport.trim(name);
                            }

                            Buffer val = line.getBuffer(seperatorIndex + 1, line.length());
                            if (trim) {
                                val = BufferSupport.trim(val);
                            }

                            String key = name.toString();
                            String value = val.toString();

                            if (key.equals(Stomp.Headers.CONTENT_LENGTH)) {
                                contentLength.set(Integer.parseInt(value));
                            }
                            headers.put(key, value);
                        } catch (Exception e) {
                            throw new IOException("Unable to parser header line [" + line + "]");
                        }

                    } else {
                        frame.setHeaders(headers);

                        if (contentLength.get() != -1) {

                            if (protocol.maxDataLength != -1 && contentLength.get() > protocol.maxDataLength) {
                                throw new IOException("The maximum data length was exceeded");
                            }

                            nextDecodeAction = read_binary_body(frame, contentLength.get());
                        } else {
                            nextDecodeAction = read_text_body(frame);
                        }
                        return nextDecodeAction.apply();
                    }
                    line = readUntil((byte) '\n', protocol.maxHeaderLength, "The maximum header length was exceeded");
                }
                return null;
            }
        };
    }

    private Action<StompFrame> read_binary_body(final StompFrame frame, final int contentLength) {
        return new Action<StompFrame>() {
            public StompFrame apply() throws IOException {
                Buffer content = readBytes(contentLength + 1);
                if (content != null) {
                    if (content.getByte(contentLength) != 0) {
                        throw new IOException("Expected null terminator after " + contentLength + " content bytes");
                    }
                    frame.setContent(BufferSupport.chomp(content).getBytes());
                    nextDecodeAction = read_action;
                    return frame;
                } else {
                    return null;
                }
            }
        };
    }

    private Action<StompFrame> read_text_body(final StompFrame frame) {
        return new Action<StompFrame>() {
            public StompFrame apply() throws IOException {
                Buffer content = readUntil((byte) 0);
                if (content != null) {
                    nextDecodeAction = read_action;
                    frame.setContent(BufferSupport.chomp(content).getBytes());
                    return frame;
                } else {
                    return null;
                }
            }
        };
    }

}
