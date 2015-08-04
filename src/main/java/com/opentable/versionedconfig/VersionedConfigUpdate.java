package com.opentable.versionedconfig;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Carries around info about files touched by git updates
 */
public final class VersionedConfigUpdate {
    private final Set<Path> alteredPaths;
    private final List<Path> allPaths;
    private final String revision;

    public VersionedConfigUpdate(Set<Path> alteredPaths, List<Path> allPaths, String revision) {
        this.alteredPaths = alteredPaths;
        this.allPaths = allPaths;
        this.revision = revision;
    }

    public Set<Path> getAlteredPaths() {
        return alteredPaths;
    }

    /**
     * order's important here!
     * @return
     */
    public List<Path> getAllPaths() {
        return allPaths;
    }

    public boolean isEmpty() {
        return alteredPaths.isEmpty();
    }

    public String getRevision() {
        return revision;
    }
}
