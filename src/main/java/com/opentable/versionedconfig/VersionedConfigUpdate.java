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

import java.nio.file.Path;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;

import org.eclipse.jgit.lib.ObjectId;

/**
 * Carries around info about files touched by git updates.
 */
public final class VersionedConfigUpdate {
    /**
     * The base path of the checked out repository.
     */
    private final Path basePath;

    /**
     * Subset of allKnownFiles affected by this update.
     */
    private final Set<Path> changedFiles;

    /**
     * Description of the VCS revisions that this change describes (e.g. SHA for git).
     */
    private final ObjectId oldRevision;
    private final ObjectId newRevision;

    public VersionedConfigUpdate(Path basePath, Iterable<Path> changedFiles) {
        this(basePath, changedFiles, ObjectId.zeroId(), ObjectId.zeroId());
    }

    public VersionedConfigUpdate(Path basePath, Iterable<Path> changedFiles, ObjectId oldRevision, ObjectId newRevision) {
        this.basePath = basePath;
        this.changedFiles = ImmutableSet.copyOf(changedFiles);
        this.oldRevision = oldRevision;
        this.newRevision = newRevision;
    }

    /**
     * @return the local base path for the checkout
     */
    public Path getBasePath() {
        return basePath;
    }

    /**
     * @return a set of the files changed in this diff
     */
    public Set<Path> getChangedFiles() {
        return changedFiles;
    }

    /**
     * @return the start revision for this diff
     */
    @Nullable
    public ObjectId getOldRevisionMetadata() {
        return oldRevision;
    }

    /**
     * @return the start revision name for this diff
     */
    public String getOldRevision() {
        return oldRevision == null ? "<unknown>" : oldRevision.getName();
    }

    /**
     * @return the end revision for this diff
     */
    @Nonnull
    public ObjectId getNewRevisionMetadata() {
        return newRevision;
    }

    /**
     * @return the end revision name for this diff
     */
    public String getNewRevision() {
        return newRevision == null ? "<unknown>" : newRevision.getName();
    }
}
