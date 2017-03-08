package com.opentable.versionedconfig;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.Optional;

public interface VersioningService extends Closeable {
    VersionedConfigUpdate getCurrentState();

    Optional<VersionedConfigUpdate> checkForUpdate() throws VersioningServiceException;

    Path getCheckoutDirectory();

    String getLatestRevision();

    static VersioningService forGitRepository(VersioningServiceProperties config) {
        return new GitService(config);
    }
}
