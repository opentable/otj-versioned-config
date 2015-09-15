package com.opentable.versionedconfig;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

public interface VersioningService {
    void setMonitoredFiles(Set<Path> pathsRelativeToCheckoutDir);

    VersionedConfigUpdate getInitialState();

    VersionedConfigUpdate getCurrentState();

    Optional<VersionedConfigUpdate> checkForUpdate() throws VersioningServiceException;

    Path getCheckoutDirectory();

    String getLatestRevision();
}
