package io.fabric8.app.library.support;

import java.util.List;

/**
 *
 */
public interface UploadManagerMBean {

    String getUploadDirectory();

    List<FileDTO> list(String parent);

    boolean delete(String parent, String filename);

}
