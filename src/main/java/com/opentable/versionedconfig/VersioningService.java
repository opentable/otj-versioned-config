package com.opentable.versionedconfig;

import java.util.function.Consumer;

public interface VersioningService {
    void readConfig(Consumer<VersionedConfigSource> configEater) throws VersioningServiceException;

    boolean checkForUpdate(Consumer<VersionedConfigSource> configEater) throws VersioningServiceException;
}
