package com.opentable.versionedconfig;

import java.nio.file.Path;
import java.util.Optional;

public interface VersioningService {
    VersionedConfigUpdate getInitialState();

    Optional<VersionedConfigUpdate> checkForUpdate() throws VersioningServiceException;

    Path getCheckoutDirectory();

    String getLatestRevision();
}
