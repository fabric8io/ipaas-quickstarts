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

package io.fabric8.mq.coordination.zookeeper;

import io.fabric8.mq.coordination.BrokerCoordinator;
import io.fabric8.utils.Systems;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreV2;
import org.apache.curator.framework.recipes.locks.Lease;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class ZooKeeperBrokerCoordinator extends ServiceSupport implements BrokerCoordinator {
    private static Logger LOG = LoggerFactory.getLogger(ZooKeeperBrokerCoordinator.class);
    private InterProcessSemaphoreV2 interProcessLock;
    private String zkConnectStr;
    private int zkRetryTime;
    private CuratorFramework curator;
    private String zkPath = "/io/fabric8/mq/controller";
    private String brokerPath;
    private Lease lease;

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

    public boolean acquireLock(long time, TimeUnit timeUnit) {
        boolean result = false;
        try {
            if (lease == null) {
                lease = interProcessLock.acquire(time, timeUnit);
                result = lease != null;
            }
        } catch (Exception e) {
            LOG.warn("Failed to get lock ", e);
        }
        return result;
    }

    public void releaseLock() {
        try {
            if (lease != null) {
                interProcessLock.returnLease(lease);
                lease = null;
            }
        } catch (Throwable e) {
            LOG.error("Failed to release InterProcessMutex", e);
        }
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        releaseLock();
        CuratorFramework curatorFramework = getCurator();
        if (curatorFramework != null) {
            curatorFramework.close();
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
        interProcessLock = new InterProcessSemaphoreV2(getCurator(), brokerPath, 1);
    }
}
