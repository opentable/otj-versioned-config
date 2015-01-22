package com.opentable.versionedconfig;

import java.io.InputStream;
import java.util.function.Consumer;

public interface VersioningService {
    void readConfig(Consumer<InputStream> streamConsumer) throws VersioningServiceException;

    boolean checkForUpdate(Consumer<InputStream> streamConsumer) throws VersioningServiceException;
}
