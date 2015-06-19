/*
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

package io.fabric8.mq.protocol;

import io.fabric8.mq.AsyncExecutors;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.vertx.java.core.impl.ConcurrentHashSet;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class BaseClientPack extends ServiceSupport {
    protected final int TIMEOUT = 5;
    protected String host = "localhost";
    protected int port = 61616;
    protected int numberOfConsumers = 2;
    protected int numberOfProducers = 2;
    protected int numberOfDestinations = 2;
    protected int numberOfMessagesPerDestination = 10;
    protected String destinationBaseName = "test.destination";
    protected boolean oneConnection = false;
    protected boolean connectionPerDestination = false;
    protected boolean connectionPerClient = true;
    private CountDownLatch producerCountDownLatch;
    private CountDownLatch consumerCountDownLatch;
    private AsyncExecutors asyncExecutors;
    private Set<Runnable> producersSet = new ConcurrentHashSet<>();
    private Set<Runnable> consumerSet = new ConcurrentHashSet<>();

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getDestinationBaseName() {
        return destinationBaseName;
    }

    public void setDestinationBaseName(String destinationBaseName) {
        this.destinationBaseName = destinationBaseName;
    }

    public int getNumberOfMessagesPerDestination() {
        return numberOfMessagesPerDestination;
    }

    public void setNumberOfMessagesPerDestination(int numberOfMessagesPerDestination) {
        this.numberOfMessagesPerDestination = numberOfMessagesPerDestination;
    }

    public int getNumberOfDestinations() {
        return numberOfDestinations;
    }

    public void setNumberOfDestinations(int numberOfDestinations) {
        this.numberOfDestinations = numberOfDestinations;
    }

    public int getNumberOfProducers() {
        return numberOfProducers;
    }

    public void setNumberOfProducers(int numberOfProducers) {
        this.numberOfProducers = numberOfProducers;
    }

    public int getNumberOfConsumers() {
        return numberOfConsumers;
    }

    public void setNumberOfConsumers(int numberOfConsumers) {
        this.numberOfConsumers = numberOfConsumers;
    }

    public boolean isConnectionPerClient() {
        return connectionPerClient;
    }

    public void setConnectionPerClient(boolean connectionPerClient) {
        this.connectionPerClient = connectionPerClient;
    }

    public boolean isOneConnection() {
        return oneConnection;
    }

    public void setOneConnection(boolean oneConnection) {
        this.oneConnection = oneConnection;
    }

    public boolean isConnectionPerDestination() {
        return connectionPerDestination;
    }

    public void setConnectionPerDestination(boolean connectionPerDestination) {
        this.connectionPerDestination = connectionPerDestination;
    }

    protected int producerProgress() {
        int total = 0;
        if (getNumberOfProducers() > 0) {
            int totalMessages = getNumberOfMessagesPerDestination() * getNumberOfDestinations();
            int count = (int) producerCountDownLatch.getCount();
            total = (((totalMessages - count) * 100)) / totalMessages;
        }
        return total;
    }

    protected int consumerProgress() {
        int total = 0;
        if (getNumberOfConsumers() > 0) {
            int totalMessages = getNumberOfMessagesPerDestination() * getNumberOfDestinations();
            int count = (int) consumerCountDownLatch.getCount();
            total = (((totalMessages - count) * 100)) / totalMessages;
        }
        return total;
    }

    public void doTheTest() throws Exception {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (getNumberOfProducers() > 0) {
                    System.err.println("Producers at " + producerProgress() + "% progress");
                }
                if (getNumberOfConsumers() > 0) {
                    System.err.println("Consumers at " + consumerProgress() + "% progress");
                }
            }
        };
        asyncExecutors.scheduleAtFixedRate(runnable, 5000, 5000);

        if (getNumberOfProducers() > 0) {
            producerCountDownLatch.await(5, TimeUnit.MINUTES);
        }
        if (getNumberOfConsumers() > 0) {
            consumerCountDownLatch.await(5, TimeUnit.MINUTES);
        }
        if (getNumberOfProducers() > 0) {
            System.err.println("Producers at " + producerProgress() + "% progress");
        }
        if (getNumberOfConsumers() > 0) {
            System.err.println("Consumers at " + consumerProgress() + "% progress");
        }
    }

    public boolean isTestFinished() {
        boolean result = false;
        if (producerCountDownLatch != null) {
            result = producerCountDownLatch.getCount() == 0;
        }
        if (consumerCountDownLatch != null) {
            result &= consumerCountDownLatch.getCount() == 0;
        }
        return result;
    }

    @Override
    protected void doStart() throws Exception {
        asyncExecutors = new AsyncExecutors();
        asyncExecutors.start();
        int totalMessages = getNumberOfMessagesPerDestination() * getNumberOfDestinations();
        if (getNumberOfProducers() > 0) {
            producerCountDownLatch = new CountDownLatch(totalMessages);
        }
        if (getNumberOfConsumers() > 0) {
            consumerCountDownLatch = new CountDownLatch(totalMessages);
        }

        for (int i = 0; i < getNumberOfDestinations(); i++) {
            String destinationName = getDestinationBaseName() + "." + i;
            if (getNumberOfConsumers() > 0) {
                for (int j = 0; j < getNumberOfConsumers(); j++) {
                    Runnable runnable = createConsumers(destinationName, consumerCountDownLatch);
                    consumerSet.add(runnable);
                }
            }

            if (getNumberOfProducers() > 0) {
                int messagesPerProducer = getNumberOfMessagesPerDestination() / getNumberOfProducers();
                for (int j = 0; j < getNumberOfProducers(); j++) {
                    Runnable runnable = createProducers(destinationName, producerCountDownLatch, messagesPerProducer);
                    producersSet.add(runnable);
                }
            }
        }
        for (Runnable runnable : consumerSet) {
            asyncExecutors.execute(runnable);
        }
        Thread.sleep(2000);
        for (Runnable runnable : producersSet) {
            asyncExecutors.execute(runnable);
        }
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        if (asyncExecutors != null) {
            asyncExecutors.stop();
        }
    }

    abstract protected Runnable createConsumers(String destinationName, CountDownLatch countDownLatch) throws Exception;

    abstract protected Runnable createProducers(String destinationName, CountDownLatch countDownLatch, int numberOfMessages) throws Exception;

    abstract protected void destroyConsumers(String destinationName) throws Exception;

    abstract protected void destroyProducers(String destinationName) throws Exception;

}
