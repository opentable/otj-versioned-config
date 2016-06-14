package com.opentable.versionedconfig;

import java.nio.file.Path;
import java.util.Set;

/**
 * Carries around info about files touched by git updates
 */
public final class VersionedConfigUpdate {
    /**
     * The base path of the checked out repository
     */
    private final Path basePath;

    /**
     * subset of allPaths affected by this update
     */
    private final Set<Path> changedFiles;

    /**
     * description of the VCS revision in which this change occurs (e.g. SHA for git)
     */
    private final String revision;

    public VersionedConfigUpdate(Path basePath, Set<Path> changedFiles, String revision) {
        this.basePath = basePath;
        this.changedFiles = changedFiles;
        this.revision = revision;
    }

    public Path getBasePath() {
        return basePath;
    }

    public Set<Path> getChangedFiles() {
        return changedFiles;
    }

    public String getRevision() {
        return revision;
    }
}
