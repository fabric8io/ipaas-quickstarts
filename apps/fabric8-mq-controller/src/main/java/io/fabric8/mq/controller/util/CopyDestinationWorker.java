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

package io.fabric8.mq.controller.util;

import io.fabric8.mq.controller.AsyncExecutors;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

public class CopyDestinationWorker extends ServiceSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(CopyDestinationWorker.class);
    private final AsyncExecutors asyncExecutors;
    private final String fromURI;
    private final String toURI;
    private final List<ActiveMQDestination> copyList = new CopyOnWriteArrayList<>();
    private final List<ActiveMQDestination> completedList = new CopyOnWriteArrayList<>();
    private ActiveMQConnection fromConnection;
    private ActiveMQConnection toConnection;
    private CountDownLatch countDownLatch;

    private Throwable error;

    public CopyDestinationWorker(AsyncExecutors asyncExecutors, String fromURI, String toURI) {
        this.asyncExecutors = asyncExecutors;
        this.fromURI = fromURI;
        this.toURI = toURI;
    }

    public void addDestinationToCopy(ActiveMQDestination destination) {
        copyList.add(destination);
    }

    public Throwable getError() {
        return error;
    }

    public boolean isError() {
        return getError() != null;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public boolean isDone() {
        return getCompletedList().size() == getCopyList().size();
    }

    public int percentageComplete() {
        if (!copyList.isEmpty() && !completedList.isEmpty()) {
            return (completedList.size() * 100) / copyList.size();
        }
        return 0;
    }

    public List<ActiveMQDestination> getCompletedList() {
        return completedList;
    }

    public List<ActiveMQDestination> getCopyList() {
        return copyList;
    }

    public void aWait() {
        if (countDownLatch != null) {
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        if (fromConnection != null) {
            fromConnection.close();
        }
        if (toConnection != null) {
            toConnection.close();
        }
    }

    @Override
    protected void doStart() throws Exception {
        countDownLatch = new CountDownLatch(getCopyList().size());
        fromConnection = (ActiveMQConnection) new ActiveMQConnectionFactory(fromURI).createConnection();
        toConnection = (ActiveMQConnection) new ActiveMQConnectionFactory(toURI).createConnection();
        toConnection.setSendAcksAsync(true);
        fromConnection.start();
        toConnection.start();
        asyncExecutors.execute(new Runnable() {
            public void run() {
                doWork();
            }
        });
    }

    private void doWork() {
        try {

            Session fromSession = fromConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Session toSession = toConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            for (ActiveMQDestination destination : getCopyList()) {
                MessageConsumer consumer = fromSession.createConsumer(destination);
                MessageProducer producer = toSession.createProducer(destination);
                Message message;
                while ((message = consumer.receive(1000)) != null) {
                    producer.send(message);
                }
                completedList.add(destination);
                countDownLatch.countDown();

                consumer.close();
                producer.close();
            }
        } catch (Throwable e) {
            LOG.error("Failed to complete copy ", e);
            setError(e);
        } finally {
            try {
                stop();
            } catch (Exception e) {
            }
        }
    }
}
