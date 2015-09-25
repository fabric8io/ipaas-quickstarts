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

import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.command.ConnectionInfo;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */

public class TransportConnectionState extends org.apache.activemq.state.ConnectionState {

    private final Object connectionMutex = new Object();
    private ConnectionContext context;
    private AtomicInteger referenceCounter = new AtomicInteger();

    public TransportConnectionState(ConnectionInfo info) {
        super(info);
    }

    public int incrementReference() {
        return referenceCounter.incrementAndGet();
    }

    public int decrementReference() {
        return referenceCounter.decrementAndGet();
    }

    public AtomicInteger getReferenceCounter() {
        return referenceCounter;
    }

    public void setReferenceCounter(AtomicInteger referenceCounter) {
        this.referenceCounter = referenceCounter;
    }
}
