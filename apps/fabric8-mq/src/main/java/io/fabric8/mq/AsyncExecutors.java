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

package io.fabric8.mq;

import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.vertx.java.core.impl.ConcurrentHashSet;

import javax.inject.Singleton;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class AsyncExecutors extends ServiceSupport {
    private final AtomicInteger executorThreadCount = new AtomicInteger();
    private final AtomicInteger schedulerThreadCount = new AtomicInteger();
    private ExecutorService executor;
    private ScheduledExecutorService scheduledExecutorService;
    private Set<MonitorScheduled> scheduledSet = new ConcurrentHashSet<>();

    public void execute(final Runnable runnable) {
        if (!isStopped()) {
            executor.execute(runnable);
        }
    }

    public ScheduledFuture scheduleAtFixedRate(Runnable runnable, long period, long maxTimeInCall) {
        MonitorScheduled monitorScheduled = new MonitorScheduled(runnable, maxTimeInCall);
        ScheduledFuture future = scheduledExecutorService.scheduleAtFixedRate(monitorScheduled, period, period, TimeUnit.MILLISECONDS);
        monitorScheduled.setFuture(future);
        scheduledSet.add(monitorScheduled);
        return future;
    }

    @Override
    protected void doStop(ServiceStopper stopper) throws Exception {
        ExecutorService executor = this.executor;
        if (executor != null) {
            executor.shutdownNow();
        }
        ScheduledExecutorService scheduledExecutorService = this.scheduledExecutorService;
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
        }
    }

    @Override
    protected void doStart() throws Exception {
        executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                             10L, TimeUnit.SECONDS,
                                             new SynchronousQueue<Runnable>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "AsyncExecutor" + executorThreadCount.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });

        scheduledExecutorService = java.util.concurrent.Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "AsyncExecutor" + schedulerThreadCount.getAndDecrement());
                t.setDaemon(true);
                return t;
            }
        });
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                for (MonitorScheduled monitorScheduled : scheduledSet) {
                    if (monitorScheduled.isStuck()) {
                        monitorScheduled.cancel();
                        scheduledSet.remove(monitorScheduled);
                    }
                }
            }
        };
        scheduledExecutorService.scheduleAtFixedRate(runnable, 10, 5, TimeUnit.SECONDS);

    }

    private class MonitorScheduled implements Runnable {
        private final Runnable target;
        private final long maxTimeInCall;
        private Future future;
        private long startTime;
        private boolean started;

        MonitorScheduled(Runnable target, long maxTimeInCall) {
            this.target = target;
            this.maxTimeInCall = maxTimeInCall;
        }

        public Future getFuture() {
            return future;
        }

        public void setFuture(Future future) {
            this.future = future;
        }

        @Override
        public void run() {
            scheduledSet.add(this);
            try {
                started = true;
                startTime = System.currentTimeMillis();
                target.run();
            } finally {
                started = false;
                scheduledSet.remove(this);
            }

        }

        public boolean isStuck() {
            return started && ((System.currentTimeMillis() - startTime) > maxTimeInCall);
        }

        public void cancel() {
            if (future != null) {
                future.cancel(true);
            }
        }

    }

}
