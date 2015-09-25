/*
 * Copyright 2005-2015 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.quickstarts.jgroups.greeter;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;

public class JGroupsGreeter extends ReceiverAdapter {
    
    private JChannel channel;
    
    public void receive(Message msg) {
        String line = msg.getSrc() + ": " + msg.getObject();
        System.out.println(line);
    }
    
    void start() throws Exception {
        channel = new JChannel(getClass().getResource("/jgroups.xml"));
        channel.setReceiver(this);
        channel.connect("greeter");
        channel.getState(null, 10000);
        eventLoop();
        channel.close();
    }

    private void eventLoop() {
        while (!Thread.interrupted()) {
            for (Address address : channel.getView().getMembers()) {
                try {
                    channel.send(address, "Hello!");
                    Thread.sleep(5 * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    //ignore
                }
            }
        }
    }
}
