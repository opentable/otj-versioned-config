package com.opentable.versionedconfig;

import java.io.File;
import java.net.URI;

import javax.annotation.Nullable;

import com.google.common.base.Objects;

/**
 * Git metadata properties.
 */
public class GitProperties {
    private final URI remoteRepository;
    private final String username;
    private final String password;
    private final File localRepository;
    private final String branch;

    public GitProperties(URI remoteRepository,
                         String username,
                         String password,
                         @Nullable File localRepository,
                         String branch) {
        this.remoteRepository = remoteRepository;
        this.username = username;
        this.password = password;
        this.localRepository = localRepository;
        this.branch = branch;
    }

    public URI getRemoteRepository() {
        return remoteRepository;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public File getLocalRepository() {
        return localRepository;
    }

    public String getBranch() {
        return branch;
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
                Objects.equal(username, that.username) &&
                Objects.equal(password, that.password) &&
                Objects.equal(localRepository, that.localRepository) &&
                Objects.equal(branch, that.branch);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(remoteRepository, username, password, localRepository, branch);
    }

    @Override
    public String toString() {
        return "GitProperties{" +
                "remoteRepository=" + remoteRepository +
                ", username='" + username + '\'' +
                ", password=<redacted>" +
                ", localRepository=" + localRepository +
                ", branch='" + branch + '\'' +
                '}';
    }
}
