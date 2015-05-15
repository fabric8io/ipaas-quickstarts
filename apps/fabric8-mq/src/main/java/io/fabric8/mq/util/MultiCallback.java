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

import org.apache.activemq.command.Command;
import org.apache.activemq.transport.FutureResponse;
import org.apache.activemq.transport.ResponseCallback;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MultiCallback implements ResponseCallback {
    private final Map<MultiCallback, MultiCallback> map;
    private final AtomicBoolean called = new AtomicBoolean();
    private final Command command;
    private final ResponseCallback callback;

    public MultiCallback(final Map<MultiCallback, MultiCallback> map, Command command, ResponseCallback callback) {
        this.map = map;
        this.command = command;
        this.callback = callback;
    }

    public ResponseCallback getCallback() {
        return callback;
    }

    public void onCompletion(FutureResponse futureResponse) {
        if (called.compareAndSet(false, true)) {
            synchronized (map) {
                map.remove(this);
            }
            callback.onCompletion(futureResponse);
        }
    }

}
