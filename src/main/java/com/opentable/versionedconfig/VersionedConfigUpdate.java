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

    public VersionedConfigUpdate(Set<Path> alteredPaths, List<Path> allPaths) {
        this.alteredPaths = alteredPaths;
        this.allPaths = allPaths;
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
}
