/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.mq.consumer;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.dataset.SimpleDataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.AccessController;
import java.security.PrivilegedAction;

public class ConsumerMain {
    private static final Logger LOG = LoggerFactory.getLogger(ConsumerMain.class);

    private static int prefetch;

    private static String queueName;

    public static void main(String args[]) {
        try {
            try {
                String prefetchStr = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        String result = System.getenv("AMQ_PREFETCH");
                        result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_PREFETCH", "1000") : result;
                        return result;
                    }
                });
                if (prefetchStr != null && prefetchStr.length() > 0) {
                    prefetch = Integer.parseInt(prefetchStr);
                }

                queueName = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        String result = System.getenv("AMQ_QUEUENAME");
                        result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_QUEUENAME", "TEST.FOO") : result;
                        return result;
                    }
                });

            } catch (Throwable e) {
                LOG.warn("Failed to look up System properties for host and port", e);
            }

            if (prefetch <= 0) {
                prefetch = 1000;
            }

            if (queueName == null) {
                queueName = "TEST.FOO";
            }

            // create a camel route to consume messages from our queue
            org.apache.camel.main.Main main = new org.apache.camel.main.Main();
            main.bind("myDataSet", new SimpleDataSet());
            main.enableHangupSupport();

            main.addRouteBuilder(new RouteBuilder() {
                public void configure() {
                    from("amq:" + queueName).to("dataset:myDataSet?retainLast=10");
                }
            });

            main.run(args);

        } catch (Throwable e) {
            LOG.error("Failed to connect to Fabric8 MQ", e);
        }
    }

    public static String getQueueName() {
        return queueName;
    }
}
