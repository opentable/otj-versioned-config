package com.opentable.versionedconfig;

import java.io.InputStream;
import java.util.function.Consumer;

public interface VersioningService
{

    public void readConfig(Consumer<InputStream> streamConsumer) throws VersioningServiceException;

    public boolean checkForUpdate(Consumer<InputStream> streamConsumer) throws VersioningServiceException;
}
