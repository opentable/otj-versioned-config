/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opentable.versionedconfig;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

/**
 * Git metadata properties.
 */
public class GitProperties {
    private final List<URI> remoteRepositories;
    private final File localRepository;
    private final String branch;

    public GitProperties(URI remoteRepository,
                         @Nullable File localRepository,
                         String branch) {
        this(Collections.singletonList(remoteRepository), localRepository, branch);
    }

    public GitProperties(List<URI> remoteRepositories,
                         @Nullable File localRepository,
                         String branch) {
        this.remoteRepositories = ImmutableList.copyOf(remoteRepositories);
        this.localRepository = localRepository;
        this.branch = branch;
    }

    public List<URI> getRemoteRepositories() {
        return remoteRepositories;
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
        return Objects.equal(remoteRepositories, that.remoteRepositories) &&
                Objects.equal(localRepository, that.localRepository) &&
                Objects.equal(branch, that.branch);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(remoteRepositories, localRepository, branch);
    }

    @Override
    public String toString() {
        return "GitProperties{" +
                "remoteRepositories=" + remoteRepositories +
                ", localRepository=" + localRepository +
                ", branch='" + branch + '\'' +
                '}';
    }
}
