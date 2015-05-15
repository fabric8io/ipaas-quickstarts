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

package io.fabric8.mq.coordination.singleton;

import io.fabric8.mq.coordination.BrokerCoordinator;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;

import java.util.concurrent.TimeUnit;

public class SingletonBrokerCoordinator extends ServiceSupport implements BrokerCoordinator {

    public boolean acquireLock(long time, TimeUnit timeUnit) {
        return true;
    }

    public void releaseLock() {
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {

    }

    @Override
    protected void doStart() throws Exception {

    }
}
