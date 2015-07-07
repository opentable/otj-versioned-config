package com.opentable.versionedconfig;

public interface VersioningService {
    VersionedConfigUpdate checkForUpdate() throws VersioningServiceException;
}
