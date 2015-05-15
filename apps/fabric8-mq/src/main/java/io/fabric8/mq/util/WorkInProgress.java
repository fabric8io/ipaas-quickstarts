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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkInProgress {
    private final AtomicBoolean working = new AtomicBoolean();
    private final AtomicInteger expectedCount = new AtomicInteger();

    public boolean startWork(int countWhenFinished) {
        boolean result = working.compareAndSet(false, true);
        if (result) {
            expectedCount.set(countWhenFinished);
        }
        return result;
    }

    public boolean isWorking() {
        return working.get();
    }

    public boolean finished(int currentCount) {
        boolean result = false;
        if (expectedCount.compareAndSet(expectedCount.get(), currentCount)) {
            working.compareAndSet(true, false);
            result = true;
        }
        return result;
    }
}
