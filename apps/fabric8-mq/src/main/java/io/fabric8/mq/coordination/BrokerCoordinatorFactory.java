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

package io.fabric8.mq.coordination;

import org.apache.activemq.util.FactoryFinder;

public class BrokerCoordinatorFactory {
    private static final FactoryFinder FINDER = new FactoryFinder("META-INF/services/io/fabric8/mq/coordination/");

    public static BrokerCoordinator getCoordinator(String type) throws Exception {
        return (BrokerCoordinator) FINDER.newInstance(type);
    }
}
