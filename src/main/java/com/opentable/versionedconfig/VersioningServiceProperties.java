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
     * @return Repo user name or auth key
     */
    @Config("config.repo.username")
    String repoUsername();

    /**
     * @return Repo password or auth type
     */
    @Config("config.repo.password")
    String repoPassword();


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
     * @return Which configuration files(s) to read, comma-separated if more than one
     */
    @Config("config.repo.file")
    String configFiles();

    /**
     * @return How often to poll for updates
     */
    @Config("config.repo.polling.interval.seconds")
    long configPollingIntervalSeconds();
}
