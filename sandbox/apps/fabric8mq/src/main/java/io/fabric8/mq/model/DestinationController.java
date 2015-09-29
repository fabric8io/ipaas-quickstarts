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
import org.apache.activemq.command.ActiveMQDestination;
import org.jboss.netty.util.internal.ConcurrentHashMap;

import java.util.Map;

public class DestinationController {
    private Map<ActiveMQDestination, DestinationControl> destinationControlMap = new ConcurrentHashMap<>();

    public void dispatched(MultiplexerInput MultiplexerInput, ActiveMQDestination destination) {
        DestinationControl destinationControl = destinationControlMap.get(destination);
        if (destinationControl == null) {
            destinationControl = new DestinationControl();
            destinationControlMap.put(destination, destinationControl);
        }
        destinationControl.dispatched(MultiplexerInput);
    }

    public void acked(MultiplexerInput MultiplexerInput, ActiveMQDestination destination) {
        DestinationControl destinationControl = destinationControlMap.get(destination);
        if (destinationControl != null) {
            destinationControl.acked(MultiplexerInput);
        }
    }

    public void unregister(MultiplexerInput MultiplexerInput) {
        for (DestinationControl destinationControl : destinationControlMap.values()) {
            destinationControl.unregister(MultiplexerInput);
        }
    }

    public void stopDispatching(ActiveMQDestination destination) {
        DestinationControl destinationControl = destinationControlMap.get(destination);
        if (destinationControl != null) {
            destinationControl.stopDispatching();
        }
    }

    public void startDispatching(ActiveMQDestination destination) {
        DestinationControl destinationControl = destinationControlMap.get(destination);
        if (destinationControl != null) {
            destinationControl.startDispatching();
        }
    }

    public boolean canDispatch(ActiveMQDestination destination) {
        boolean result = true;
        DestinationControl destinationControl = destinationControlMap.get(destination);
        if (destinationControl != null) {
            result = destinationControl.canDispatch();
        }
        return result;
    }

    public boolean isStoppedDispatching(ActiveMQDestination destination) {
        boolean result = true;
        DestinationControl destinationControl = destinationControlMap.get(destination);
        if (destinationControl != null) {
            result = destinationControl.isStoppedDispatching();
        }
        return result;
    }

    public DestinationControl getDesinationControl(ActiveMQDestination destination) {
        return destinationControlMap.get(destination);
    }
}
