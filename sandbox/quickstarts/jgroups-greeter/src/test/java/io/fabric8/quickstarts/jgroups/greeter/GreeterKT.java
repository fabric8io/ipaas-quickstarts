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


import io.fabric8.utils.Asserts;
import io.fabric8.utils.Block;
import org.jboss.arquillian.junit.Arquillian;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class GreeterKT {

    @Test
    public void testChat() throws Exception {
        final JChannel channel = new JChannel(getClass().getResource("/jgroups.xml"));
        channel.setReceiver(new ReceiverAdapter() {
            @Override
            public void receive(Message msg) {
                String line = "Message From:" + msg.getSrc() + ". Body:" + msg.getObject();
                System.out.println(line);
            }
        });
        channel.connect("greeter");
        channel.getState(null, 10000);
        Asserts.assertWaitFor(2 * 60 * 1000, new Block() {
            @Override
            public void invoke() throws Exception {
                for (Address address : channel.getView().getMembers()) {
                    System.out.println(address);
                }
                Assert.assertTrue(channel.getView().getMembers().size() > 3);
            }
        });
    }
}
