package com.opentable.versionedconfig;

import static java.util.stream.Collectors.toSet;

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
     * All paths known by the versioning service
     */
    private final Set<Path> allKnownFiles;

    /**
     * subset of allPaths affected by this update
     */
    private final Set<Path> changedFiles;

    /**
     * description of the VCS revision in which this change occurs (e.g. SHA for git)
     */
    private final String revision;

    public VersionedConfigUpdate(Path basePath, Set<Path> changedFiles, Set<Path> allKnownFiles, String revision) {
        this.basePath = basePath;
        this.changedFiles = changedFiles;
        this.allKnownFiles = allKnownFiles;
        this.revision = revision;
    }

    public Path getBasePath() {
        return basePath;
    }

    public Set<Path> getChangedFiles() {
        return changedFiles;
    }

    public Set<Path> getAllKnownFiles() {
        return allKnownFiles;
    }

    public String getRevision() {
        return revision;
    }

    /**
     * @return all known paths as absolute paths (i.e. including basePath)
     */
    public Set<Path> getAllAbsolutePaths() {
        return allKnownFiles.stream().map(basePath::resolve).collect(toSet());
    }

}
