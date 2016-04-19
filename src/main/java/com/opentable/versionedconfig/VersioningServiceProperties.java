package com.opentable.versionedconfig;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.DefaultNull;

interface VersioningServiceProperties
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
    @Default("x-oauth-basic")
    String repoPassword();


    /**
     * Where the configuration should be cloned to, in the local filesystem.
     * If left to the null default, will create a temporary checkout that is destroyed
     * on close.
     */
    @Config("config.repo.local")
    @DefaultNull
    File localConfigRepository();

    /**
     * @return Which configuration branch to read
     */
    @Config("config.repo.branch")
    @Default("master")
    String configBranch();

    /**
     * @return Which configuration files(s) to read, comma-separated if more than one
     */
    @Config("config.repo.file")
    @Default("")
    List<String> configFiles();
}
