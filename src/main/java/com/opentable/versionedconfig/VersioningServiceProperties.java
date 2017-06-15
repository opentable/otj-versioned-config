package com.opentable.versionedconfig;

import java.io.File;
import java.net.URI;

import org.springframework.beans.factory.annotation.Value;

/**
 * @deprecated use {@link com.opentable.versionedconfig.GitProperties} instead.
 */
@Deprecated
class VersioningServiceProperties {
    /**
     * @return Where the configuration should be cloned from
     */
    @Value("${config.repo.remote}")
    private URI remoteConfigRepository;

    /**
     * @return Repo user name or auth key
     */
    @Value("${config.repo.username}")
    private String repoUsername;

    /**
     * @return Repo password or auth type
     */
    @Value("${config.repo.password:x-oauth-basic}")
    private String repoPassword = "x-oauth-basic";

    /**
     * Where the configuration should be cloned to, in the local filesystem.
     * If left to the null default, will create a temporary checkout that is destroyed
     * on close.
     */
    @Value("${config.repo.local:#{null}}")
    private File localConfigRepository;

    /**
     * @return Which configuration branch to read
     */
    @Value("${config.repo.branch:master}")
    private String configBranch = "master";

    public VersioningServiceProperties setRemoteConfigRepository(URI remoteConfigRepository) {
        this.remoteConfigRepository = remoteConfigRepository;
        return this;
    }

    public VersioningServiceProperties setRepoUsername(String repoUsername) {
        this.repoUsername = repoUsername;
        return this;
    }

    public VersioningServiceProperties setRepoPassword(String repoPassword) {
        this.repoPassword = repoPassword;
        return this;
    }

    public VersioningServiceProperties setLocalConfigRepository(File localConfigRepository) {
        this.localConfigRepository = localConfigRepository;
        return this;
    }

    public VersioningServiceProperties setConfigBranch(String configBranch) {
        this.configBranch = configBranch;
        return this;
    }

    public URI getRemoteConfigRepository() {
        return remoteConfigRepository;
    }

    public String getRepoUsername() {
        return repoUsername;
    }

    public String getRepoPassword() {
        return repoPassword;
    }

    public File getLocalConfigRepository() {
        return localConfigRepository;
    }

    public String getConfigBranch() {
        return configBranch;
    }

    public static VersioningServiceProperties fromGitProperties(GitProperties config) {
        VersioningServiceProperties props = new VersioningServiceProperties();
        props.setRemoteConfigRepository(config.getRemoteRepository());
        props.setRepoUsername(config.getRepoUsername());
        props.setRepoPassword(config.getRepoPassword());
        props.setLocalConfigRepository(config.getLocalConfigRepository());
        props.setConfigBranch(config.getConfigBranch());
        return props;
    }
}
