package com.opentable.versionedconfig;

import java.io.InputStream;

public final class VersionedConfigSource {
    private final InputStream stream;
    private final String description;

    public VersionedConfigSource(InputStream stream, String description) {
        this.stream = stream;
        this.description = description;

    }

    public InputStream getStream() {
        return stream;
    }

    public String getDescription() {
        return description;
    }
}
