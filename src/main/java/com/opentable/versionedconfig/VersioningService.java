package com.opentable.versionedconfig;

import java.io.Closeable;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

public interface VersioningService extends Closeable {
    VersionedConfigUpdate getCurrentState();

    Optional<VersionedConfigUpdate> checkForUpdate() throws VersioningServiceException;

    Path getCheckoutDirectory();

    String getLatestRevision();

    URI getRemoteRepository();

    String getBranch();

    static VersioningService forGitRepository(GitProperties config) {
        return new GitService(config);
    }
}
