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

package io.fabric8.amq;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.jmx.ManagementContext;
import org.apache.activemq.store.kahadb.KahaDBPersistenceAdapter;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.apache.deltaspike.core.api.config.ConfigProperty;

import javax.inject.Inject;
import javax.management.MBeanServer;
import java.io.File;
import java.lang.management.ManagementFactory;

public class AMQBroker extends ServiceSupport {
    @Inject
    @ConfigProperty(name = "AMQ_BROKER_NAME", defaultValue = "defaultBroker")
    private String brokerName;
    @Inject
    @ConfigProperty(name = "AMQ_DATA_DIRECTORY", defaultValue = "data")
    private String dataDirectory;
    @Inject
    @ConfigProperty(name = "AMQ_HOST", defaultValue = "0.0.0.0")
    private String host;
    @Inject
    @ConfigProperty(name = "AMQ_PORT", defaultValue = "61616")
    private int port;
    @Inject
    @ConfigProperty(name = "PERSISTENT", defaultValue = "false")
    private boolean persistent = false;
    @Inject
    @ConfigProperty(name = "DELETE_ALL_MESSAGES_ON_START", defaultValue = "false")
    private boolean deleteAllMessagesOnStartup;
    @Inject
    @ConfigProperty(name = "ADVISORY_SUPPORT", defaultValue = "true")
    private boolean advisorySupport = true;
    @Inject
    @ConfigProperty(name = "CONCURRENT_STORE_AND_DISPATCH_QUEUES", defaultValue = "true")
    private boolean concurrentStoreAndDispatchQueues = true;
    @Inject
    @ConfigProperty(name = "CONCURRENT_STORE_AND_DISPATCH_TOPICS", defaultValue = "false")
    private boolean concurrentStoreAndDispatchTopics = false;
    @Inject
    @ConfigProperty(name = "ENABLE_JOURNAL_DISK_SYNC", defaultValue = "false")
    private boolean enableJournalDiskSyncs=false;
    @Inject
    @ConfigProperty(name = "JOURNAL_CHECK_POINT_INTERVAL", defaultValue = "5000")
    private long checkpointInterval = 5*1000;
    @Inject
    @ConfigProperty(name = "JOURNAL_CLEANUP_INTERVAL", defaultValue = "30000")
    private long cleanupInterval = 30*1000;
    @Inject
    @ConfigProperty(name = "JOURNAL_MAX_FILE_LENGTH", defaultValue = ""+ (1024*1024*32))
    private int journalMaxFileLength = 1024 * 1024 * 32;
    @Inject
    @ConfigProperty(name = "ENBALE_INDEX_WRITE_ASYNC", defaultValue = "false")
    private boolean enableIndexWriteAsync = false;
    @Inject
    @ConfigProperty(name = "IGNORE_MISSING_JOURNAL_FILES", defaultValue = "false")
    private boolean ignoreMissingJournalfiles = false;
    @Inject
    @ConfigProperty(name = "CHECK_FOR_CORRUPTED_JOURNAL_FILES", defaultValue = "false")
    private boolean checkForCorruptJournalFiles = false;
    @Inject
    @ConfigProperty(name = "CHECKSUM_JOURNAL_FILES", defaultValue = "true")
    private boolean checksumJournalFiles = true;
    @Inject
    @ConfigProperty(name = "FORCE_RECOVER_INDEX", defaultValue = "true")
    private boolean forceRecoverIndex = false;

    private BrokerService brokerService;

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public String getDataDirectory() {
        return dataDirectory;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public boolean isDeleteAllMessagesOnStartup() {
        return deleteAllMessagesOnStartup;
    }

    public void setDeleteAllMessagesOnStartup(boolean deleteAllMessagesOnStartup) {
        this.deleteAllMessagesOnStartup = deleteAllMessagesOnStartup;
    }

    public boolean isAdvisorySupport() {
        return advisorySupport;
    }

    public void setAdvisorySupport(boolean advisorySupport) {
        this.advisorySupport = advisorySupport;
    }

    public boolean isConcurrentStoreAndDispatchQueues() {
        return concurrentStoreAndDispatchQueues;
    }

    public void setConcurrentStoreAndDispatchQueues(boolean concurrentStoreAndDispatchQueues) {
        this.concurrentStoreAndDispatchQueues = concurrentStoreAndDispatchQueues;
    }

    public boolean isConcurrentStoreAndDispatchTopics() {
        return concurrentStoreAndDispatchTopics;
    }

    public void setConcurrentStoreAndDispatchTopics(boolean concurrentStoreAndDispatchTopics) {
        this.concurrentStoreAndDispatchTopics = concurrentStoreAndDispatchTopics;
    }


    public boolean isEnableJournalDiskSyncs() {
        return enableJournalDiskSyncs;
    }

    public void setEnableJournalDiskSyncs(boolean enableJournalDiskSyncs) {
        this.enableJournalDiskSyncs = enableJournalDiskSyncs;
    }

    public long getCheckpointInterval() {
        return checkpointInterval;
    }

    public void setCheckpointInterval(long checkpointInterval) {
        this.checkpointInterval = checkpointInterval;
    }

    public long getCleanupInterval() {
        return cleanupInterval;
    }

    public void setCleanupInterval(long cleanupInterval) {
        this.cleanupInterval = cleanupInterval;
    }

    public int getJournalMaxFileLength() {
        return journalMaxFileLength;
    }

    public void setJournalMaxFileLength(int journalMaxFileLength) {
        this.journalMaxFileLength = journalMaxFileLength;
    }


    public boolean isEnableIndexWriteAsync() {
        return enableIndexWriteAsync;
    }

    public void setEnableIndexWriteAsync(boolean enableIndexWriteAsync) {
        this.enableIndexWriteAsync = enableIndexWriteAsync;
    }


    public boolean isIgnoreMissingJournalfiles() {
        return ignoreMissingJournalfiles;
    }

    public void setIgnoreMissingJournalfiles(boolean ignoreMissingJournalfiles) {
        this.ignoreMissingJournalfiles = ignoreMissingJournalfiles;
    }


    public boolean isCheckForCorruptJournalFiles() {
        return checkForCorruptJournalFiles;
    }

    public void setCheckForCorruptJournalFiles(boolean checkForCorruptJournalFiles) {
        this.checkForCorruptJournalFiles = checkForCorruptJournalFiles;
    }

    public boolean isChecksumJournalFiles() {
        return checksumJournalFiles;
    }

    public void setChecksumJournalFiles(boolean checksumJournalFiles) {
        this.checksumJournalFiles = checksumJournalFiles;
    }

    public boolean isForceRecoverIndex() {
        return forceRecoverIndex;
    }

    public void setForceRecoverIndex(boolean forceRecoverIndex) {
        this.forceRecoverIndex = forceRecoverIndex;
    }

    @Override
    protected void doStart() throws Exception{
        brokerService = new BrokerService();

        long maxMemory = Runtime.getRuntime().maxMemory();
        long brokerMemory = (long) (maxMemory * 0.7);

        brokerService.getSystemUsage().getMemoryUsage().setLimit(brokerMemory);
        brokerService.setBrokerName(getBrokerName());
        brokerService.setAdvisorySupport(isAdvisorySupport());
        if (isPersistent()) {
            brokerService.setDataDirectory(getDataDirectory());
            KahaDBPersistenceAdapter persistenceAdapter = new KahaDBPersistenceAdapter();
            persistenceAdapter.setDirectory(new File(getDataDirectory()));
            persistenceAdapter.setCheckForCorruptJournalFiles(isCheckForCorruptJournalFiles());
            persistenceAdapter.setCheckpointInterval(getCheckpointInterval());
            persistenceAdapter.setChecksumJournalFiles(isChecksumJournalFiles());
            persistenceAdapter.setCleanupInterval(getCleanupInterval());
            persistenceAdapter.setConcurrentStoreAndDispatchQueues(isConcurrentStoreAndDispatchQueues());
            persistenceAdapter.setConcurrentStoreAndDispatchTopics(isConcurrentStoreAndDispatchTopics());
            persistenceAdapter.setEnableIndexDiskSyncs(!isEnableIndexWriteAsync());
            persistenceAdapter.setEnableJournalDiskSyncs(isEnableJournalDiskSyncs());
            persistenceAdapter.setJournalMaxFileLength(getJournalMaxFileLength());
            persistenceAdapter.setIgnoreMissingJournalfiles(isIgnoreMissingJournalfiles());
            persistenceAdapter.setForceRecoverIndex(isForceRecoverIndex());
                brokerService.setDeleteAllMessagesOnStartup(isDeleteAllMessagesOnStartup());
            brokerService.setPersistenceAdapter(persistenceAdapter);
        }

        //we create our own ManagementContext - so ActiveMQ doesn't create a needless JMX Connector
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        ManagementContext managementContext = new ManagementContext(server);
        managementContext.setCreateConnector(false);
        brokerService.setManagementContext(managementContext);

        String connector = "tcp://" + getHost() + ":" + getPort();
        System.out.println("Starting AMQ Broker on " + connector);
        brokerService.addConnector(connector);
        brokerService.start();


    }

    @Override
    protected void doStop(ServiceStopper stopper) throws Exception{
        if (brokerService != null){
            stopper.stop(brokerService);
        }
    }
}
