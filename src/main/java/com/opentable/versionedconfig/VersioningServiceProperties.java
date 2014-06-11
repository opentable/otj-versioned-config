package com.opentable.versionedconfig;

import java.net.URI;

import org.skife.config.Config;

public interface VersioningServiceProperties
{
    /**
     * @return Where the configuration should be cloned from
     */
    @Config("config.repo.remote")
    URI remoteConfigRepository();

    /**
     * @return Where the configuration should be cloned to, in the local filesystem
     */
    @Config("config.repo.local")
    URI localConfigRepository();

    /**
     * @return Which configuration branch to read
     */
    @Config("config.repo.branch")
    String configBranch();

    /**
     * @return Which configuration branch to read
     */
    @Config("config.repo.file")
    String configFile();

    /**
     * @return How often to pull for updates
     */
    @Config("config.repo.polling.interval.seconds")
    long configPollingIntervalSeconds();

    /**
     * @return Which path to probe when polling for updates
     */
    @Config("repo.polling.path.probe")
    String pollingProbePath();
}
