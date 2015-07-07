package com.opentable.versionedconfig;

import java.io.File;

import com.google.common.collect.ImmutableSet;

public final class VersionedConfigUpdate {
    private final ImmutableSet<File> alteredPaths;

    public VersionedConfigUpdate(ImmutableSet<File> alteredPaths) {
        this.alteredPaths = alteredPaths;
    }

    public ImmutableSet<File> getAlteredPaths() {
        return alteredPaths;
    }

    static final VersionedConfigUpdate NO_AFFECTED_FILES = new VersionedConfigUpdate(ImmutableSet.<File>of());
}
