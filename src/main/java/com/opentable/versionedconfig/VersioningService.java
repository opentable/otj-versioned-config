package com.opentable.versionedconfig;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface VersioningService {
    void setAllMonitoredPaths(Set<Path> pathsRelativeToCheckoutDir);

    VersionedConfigUpdate getInitialState();

    VersionedConfigUpdate getCurrentState();

    Optional<VersionedConfigUpdate> checkForUpdate() throws VersioningServiceException;

    Path getCheckoutDirectory();

    String getLatestRevision();

    List<Path> getInitialPaths();
}
