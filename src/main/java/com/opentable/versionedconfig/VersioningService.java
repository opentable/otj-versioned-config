package com.opentable.versionedconfig;

import java.util.function.Consumer;

public interface VersioningService {
    VersionedConfigUpdate checkForUpdate(Consumer<VersionedConfigUpdate> configEater) throws VersioningServiceException;
}
