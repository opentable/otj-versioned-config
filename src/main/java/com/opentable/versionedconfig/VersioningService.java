package com.opentable.versionedconfig;

import java.io.File;
import java.util.Set;
import java.util.function.Consumer;

public interface VersioningService {
    Set<File> checkForUpdate(Consumer<ConfigUpdate> configEater) throws VersioningServiceException;
}
