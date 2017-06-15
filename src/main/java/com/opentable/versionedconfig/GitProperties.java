package com.opentable.versionedconfig;

import java.io.File;
import java.net.URI;

public class GitProperties {
    private final URI remoteConfigRepository;
    private final String repoUsername;
    private final String repoPassword;
    private final File localConfigRepository;
    private final String configBranch;

    public GitProperties(URI remoteConfigRepository,
                         String repoUsername,
                         String repoPassword,
                         File localConfigRepository,
                         String configBranch) {
        this.remoteConfigRepository = remoteConfigRepository;
        this.repoUsername = repoUsername;
        this.repoPassword = repoPassword;
        this.localConfigRepository = localConfigRepository;
        this.configBranch = configBranch;
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
}
