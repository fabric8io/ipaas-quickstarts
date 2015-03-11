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

package io.fabric8.mq.controller.coordination.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.mq.controller.coordination.BrokerChangeListener;
import io.fabric8.mq.controller.coordination.BrokerCoordinator;
import io.fabric8.mq.controller.coordination.BrokerView;
import io.fabric8.utils.Systems;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ZooKeeperBrokerCoordinator extends ServiceSupport implements BrokerCoordinator {
    private static Logger LOG = LoggerFactory.getLogger(ZooKeeperBrokerCoordinator.class);
    private final List<BrokerChangeListener> brokerChangeListenerList = new CopyOnWriteArrayList<>();
    private PathChildrenCache cache;
    private InterProcessMutex interProcessMutex;
    private final ReadWriteLock localLock = new ReentrantReadWriteLock(true);
    private PathChildrenCacheListener pathChildrenCacheListener;
    private String zkConnectStr;
    private int zkRetryTime;
    private CuratorFramework curator;
    private String zkPath = "/io/fabric8/mq/controller";
    private String brokerPath;

    public String getZkConnectStr() {
        return zkConnectStr;
    }

    public void setZkConnectStr(String zkConnectStr) {
        this.zkConnectStr = zkConnectStr;
    }

    public int getZkRetryTime() {
        return zkRetryTime;
    }

    public void setZkRetryTime(int zkRetryTime) {
        this.zkRetryTime = zkRetryTime;
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    public String getZkPath() {
        return zkPath;
    }

    public void setZkPath(String zkPath) {
        this.zkPath = zkPath;
    }

    public void addBrokerChangeListener(BrokerChangeListener brokerChangeListener) {
        brokerChangeListenerList.add(brokerChangeListener);
    }

    public void removeBrokerChangeListener(BrokerChangeListener brokerChangeListener) {
        brokerChangeListenerList.add(brokerChangeListener);
    }

    public void createBroker(BrokerView brokerView) {
        boolean created = false;
        try {
            if (acquireWriteLock()) {
                //update ZK
                String path = ZKPaths.makePath(brokerPath, brokerView.getBrokerName());
                ZKPaths.mkdirs(getCurator().getZookeeperClient().getZooKeeper(), path);
                ObjectMapper objectMapper = new ObjectMapper();
                byte[] data = objectMapper.writeValueAsBytes(brokerView);
                getCurator().setData().forPath(path, data);
                created = true;
            }
        } catch (Throwable e) {
            LOG.error("Failed to create Broker " + brokerView);
        } finally {
            releaseWriteLock();
        }
        if (created) {
            fireBrokerCreated(brokerView);
        }
    }

    public void updateBroker(BrokerView brokerView) {
        boolean updated = false;
        try {
            if (acquireWriteLock()) {
                //update ZK
                String path = ZKPaths.makePath(brokerPath, brokerView.getBrokerName());
                ZKPaths.mkdirs(getCurator().getZookeeperClient().getZooKeeper(), path);
                ObjectMapper objectMapper = new ObjectMapper();
                byte[] data = objectMapper.writeValueAsBytes(brokerView);
                getCurator().setData().forPath(path, data);
                updated = true;
            }
        } catch (Throwable e) {
            LOG.error("Failed to create Broker " + brokerView);
        } finally {
            releaseWriteLock();
        }
        if (updated) {
            fireBrokerUpdated(brokerView);
        }
    }

    public void deleteBroker(BrokerView brokerView) {

        try {
            if (acquireWriteLock()) {
                String path = ZKPaths.makePath(getZkPath(), brokerView.getBrokerName());
                getCurator().delete().forPath(path);
                fireBrokerDeleted(brokerView.getBrokerName());
            }
        } catch (Throwable e) {
            LOG.error("Failed to delete broker " + brokerView);
        } finally {
            releaseWriteLock();
        }
    }

    private boolean acquireWriteLock() throws Exception {
        boolean result = false;
        if (interProcessMutex.acquire(LOCK_TIME, TimeUnit.MILLISECONDS)) {
            result = localLock.writeLock().tryLock(LOCK_TIME, TimeUnit.MILLISECONDS);
        }
        return result;
    }

    private void releaseWriteLock() {
        try {
            interProcessMutex.release();
        } catch (Throwable e) {
            LOG.error("Failed to release InterProcessMutex", e);
        }
        try {
            localLock.writeLock().unlock();
        } catch (Throwable e) {
            LOG.error("Failed to release localLock", e);
        }
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        CuratorFramework curatorFramework = getCurator();
        if (curatorFramework != null) {
            curatorFramework.close();
        }
        PathChildrenCache cache = this.cache;
        if (cache != null) {
            PathChildrenCacheListener l = pathChildrenCacheListener;
            if (l != null) {
                cache.getListenable().removeListener(l);
            }
            cache.close();
        }
    }

    @Override
    protected void doStart() throws Exception {
        CuratorFramework curatorFramework = getCurator();
        if (curatorFramework == null) {
            String connectStr = getZkConnectStr();
            if (connectStr == null || connectStr.isEmpty()) {
                connectStr = Systems.getEnvVarOrSystemProperty("ZK_CONNECT_STR", "127.0.0.1:2181");
                setZkConnectStr(connectStr);
            }
            int retryTime = getZkRetryTime();
            if (retryTime == 0) {
                retryTime = Systems.getEnvVarOrSystemProperty("ZK_RETRY_TIME", 2000).intValue();
                setZkRetryTime(retryTime);
            }
            curatorFramework = CuratorFrameworkFactory.newClient(getZkConnectStr(), new RetryOneTime(getZkRetryTime()));
            curatorFramework.start();
            setCurator(curatorFramework);
        }
        String zkPath = Systems.getEnvVarOrSystemProperty("ZK_PATH", getZkPath());
        setZkPath(zkPath);
        brokerPath = ZKPaths.makePath(getZkPath(), "brokers");
        ZKPaths.mkdirs(getCurator().getZookeeperClient().getZooKeeper(), brokerPath);
        cache = new PathChildrenCache(getCurator(), brokerPath, true);
        cache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);

        pathChildrenCacheListener = new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent event) throws Exception {
                if (!isStopping() && event != null && event.getData() != null) {
                    byte[] data = event.getData().getData();
                    if (data != null && data.length > 0) {
                        switch (event.getType()) {
                            case CHILD_ADDED: {
                                BrokerView brokerView = getFromData(data);
                                fireBrokerCreated(brokerView);
                                break;
                            }

                            case CHILD_UPDATED: {
                                BrokerView brokerView = getFromData(data);
                                fireBrokerUpdated(brokerView);
                                break;
                            }

                            case CHILD_REMOVED: {
                                BrokerView brokerView = getFromData(data);
                                fireBrokerDeleted(brokerView.getBrokerName());
                                break;
                            }
                        }
                    }
                }
            }
        };
        cache.getListenable().addListener(pathChildrenCacheListener);

        //build initial data
        for (ChildData childData : cache.getCurrentData()) {
            BrokerView brokerView = getFromData(childData.getData());
            fireBrokerCreated(brokerView);
        }
        interProcessMutex = new InterProcessMutex(getCurator(), getZkPath());

    }

    private BrokerView getFromData(byte[] data) {
        BrokerView result = null;
        try {
            if (data != null) {
                ObjectMapper objectMapper = new ObjectMapper();
                result = objectMapper.readValue(data, BrokerView.class);
            }
        } catch (Throwable e) {
            LOG.error("Failed to update BrokerInfo ", e);
        }
        return result;
    }

    private void fireBrokerCreated(BrokerView view) {
        for (BrokerChangeListener brokerChangeListener : brokerChangeListenerList) {
            brokerChangeListener.created(view);
        }
    }

    private void fireBrokerUpdated(BrokerView view) {
        for (BrokerChangeListener brokerChangeListener : brokerChangeListenerList) {
            brokerChangeListener.updated(view);
        }
    }

    private void fireBrokerDeleted(String name) {
        for (BrokerChangeListener brokerChangeListener : brokerChangeListenerList) {
            brokerChangeListener.deleted(name);
        }
    }
}
