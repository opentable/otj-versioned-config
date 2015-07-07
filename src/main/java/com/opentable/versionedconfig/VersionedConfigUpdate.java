package com.opentable.versionedconfig;

import java.io.File;
import java.util.Set;

public final class VersionedConfigUpdate {
    private final Set<File> alteredPaths;

    public VersionedConfigUpdate(Set<File> alteredPaths) {
        this.alteredPaths = alteredPaths;
    }

    public Set<File> getAlteredPaths() {
        return alteredPaths;
    }
}
