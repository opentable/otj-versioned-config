package com.opentable.versionedconfig;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface VersioningService {
    void setMonitoredFiles(List<String> paths);

    VersionedConfigUpdate getInitialState();

    Optional<VersionedConfigUpdate> checkForUpdate() throws VersioningServiceException;

    Path getCheckoutDirectory();

    String getLatestRevision();
}
