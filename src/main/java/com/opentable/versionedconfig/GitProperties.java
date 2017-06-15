package com.opentable.versionedconfig;

import java.io.File;
import java.net.URI;

import javax.annotation.Nullable;

import com.google.common.base.Objects;

public class GitProperties {
    private final URI remoteRepository;
    private final String repoUsername;
    private final String repoPassword;
    private final File localConfigRepository;
    private final String configBranch;

    public GitProperties(URI remoteRepository,
                         String repoUsername,
                         String repoPassword,
                         @Nullable File localConfigRepository,
                         String configBranch) {
        this.remoteRepository = remoteRepository;
        this.repoUsername = repoUsername;
        this.repoPassword = repoPassword;
        this.localConfigRepository = localConfigRepository;
        this.configBranch = configBranch;
    }

    public URI getRemoteRepository() {
        return remoteRepository;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GitProperties that = (GitProperties) o;
        return Objects.equal(remoteRepository, that.remoteRepository) &&
                Objects.equal(repoUsername, that.repoUsername) &&
                Objects.equal(repoPassword, that.repoPassword) &&
                Objects.equal(localConfigRepository, that.localConfigRepository) &&
                Objects.equal(configBranch, that.configBranch);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(remoteRepository, repoUsername, repoPassword, localConfigRepository, configBranch);
    }

    @Override
    public String toString() {
        return "GitProperties{" +
                "remoteRepository=" + remoteRepository +
                ", repoUsername='" + repoUsername + '\'' +
                ", repoPassword=<redacted>" +
                ", localConfigRepository=" + localConfigRepository +
                ", configBranch='" + configBranch + '\'' +
                '}';
    }
}
