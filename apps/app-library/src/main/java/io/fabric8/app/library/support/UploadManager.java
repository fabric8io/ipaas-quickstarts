package io.fabric8.app.library.support;

import io.hawt.util.Strings;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.FileCleanerCleanup;
import org.apache.commons.io.FileCleaningTracker;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.ServletContext;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class UploadManager implements UploadManagerMBean {

    private static final transient Logger LOG = LoggerFactory.getLogger(UploadManager.class);
    public static String UPLOAD_DIRECTORY = "";

    private ObjectName objectName;
    private MBeanServer mBeanServer;

    public static DiskFileItemFactory newDiskFileItemFactory(ServletContext context, File repository) {
        FileCleaningTracker fileCleaningTracker = FileCleanerCleanup.getFileCleaningTracker(context);
        DiskFileItemFactory factory = new DiskFileItemFactory(DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD, repository);
        factory.setFileCleaningTracker(fileCleaningTracker);
        return factory;
    }

    /**
     * @param uploadDirectory the file system directory used for uploading new files into the library
     * @throws Exception
     */
    @Inject
    public UploadManager(@ConfigProperty(name = "UPLOAD_DIRECTORY", defaultValue = "") String uploadDirectory) throws Exception {
        if (Strings.isBlank(uploadDirectory)) {
            uploadDirectory = System.getProperty("java.io.tmpdir") + File.separator + "uploads";
        }
        UploadManager.UPLOAD_DIRECTORY = uploadDirectory;
        LOG.info("Using file upload directory: {}", UploadManager.UPLOAD_DIRECTORY);

        if (objectName == null) {
            objectName = getObjectName();
        }

        if (mBeanServer == null) {
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
        }

        if (mBeanServer != null) {
            try {
                mBeanServer.registerMBean(this, objectName);
            } catch (InstanceAlreadyExistsException iaee) {
                // Try to remove and re-register
                mBeanServer.unregisterMBean(objectName);
                mBeanServer.registerMBean(this, objectName);
            }
        }
    }

    @PreDestroy
    public void destroy() throws Exception {
        if (mBeanServer != null) {
            if (objectName != null) {
                mBeanServer.unregisterMBean(objectName);
            }
        }
    }

    protected ObjectName getObjectName() throws Exception {
        return new ObjectName("hawtio:type=UploadManager");
    }

    @Override
    public String getUploadDirectory() {
        return UPLOAD_DIRECTORY;
    }

    public void setUploadDirectory(String directory) {
        this.UPLOAD_DIRECTORY = directory;
    }

    @Override
    public List<FileDTO> list(String parent) {
        File dir = new File(getTargetDirectory(parent));
        if (!dir.exists()) {
            return null;
        }

        List<FileDTO> rc = new ArrayList<FileDTO>();

        for (File file : dir.listFiles()) {
            if (!file.isDirectory()) {
                rc.add(new FileDTO(file));
            }
        }
        return rc;
    }

    private String getTargetDirectory(String parent) {
        parent = Strings.sanitizeDirectory(parent);
        if (Strings.isNotBlank(parent)) {
            return UPLOAD_DIRECTORY + File.separator + parent;
        }
        return UPLOAD_DIRECTORY;
    }

    @Override
    public boolean delete(String parent, String filename) {
        filename = Strings.sanitize(filename);
        File targetFile = new File(getTargetDirectory(parent), filename);
        LOG.info("Deleting {}", targetFile);
        return targetFile.delete();
    }

}
