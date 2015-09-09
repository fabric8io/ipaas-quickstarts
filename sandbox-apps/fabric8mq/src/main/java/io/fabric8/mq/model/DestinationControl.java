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

package io.fabric8.mq.model;

import io.fabric8.mq.multiplexer.MultiplexerInput;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DestinationControl {
    private Map<MultiplexerInput, AtomicInteger> inflightMap = new ConcurrentHashMap<>();
    private AtomicBoolean canDispatch = new AtomicBoolean(true);

    public void unregister(MultiplexerInput MultiplexerInput) {
        inflightMap.remove(MultiplexerInput);
    }

    public synchronized void dispatched(MultiplexerInput dispatcher) {
        AtomicInteger integer = inflightMap.get(dispatcher);
        if (integer == null) {
            integer = new AtomicInteger();
            inflightMap.put(dispatcher, integer);
        }
        integer.incrementAndGet();

    }

    public void acked(MultiplexerInput dispatcher) {
        AtomicInteger integer = inflightMap.get(dispatcher);
        if (integer != null) {
            integer.decrementAndGet();
        }
    }

    public void stopDispatching() {
        canDispatch.set(false);
    }

    public void startDispatching() {
        canDispatch.set(true);
    }

    public boolean canDispatch() {
        return canDispatch.get();
    }

    public boolean isStoppedDispatching() {
        boolean result = true;
        for (AtomicInteger integer : inflightMap.values()) {
            if (integer.get() > 0) {
                result = false;
                break;
            }
        }
        return result;
    }

    public int getTotalInFlight() {
        int result = 0;
        for (AtomicInteger integer : inflightMap.values()) {
            result += integer.get();
        }
        return result;
    }
}
