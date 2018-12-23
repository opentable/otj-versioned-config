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

import static java.util.Optional.empty;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;
import javax.annotation.concurrent.NotThreadSafe;
import javax.inject.Inject;

import org.eclipse.jgit.lib.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opentable.io.DeleteRecursively;

/**
 * Responsible for noticing when service configuration has been updated.
 */
@NotThreadSafe
class GitService implements VersioningService {

    private static final Logger LOG = LoggerFactory.getLogger(GitService.class);

    private final Path checkoutDirectory;
    private final GitProperties config;

    private final GitOperations gitOperations;

    private final AtomicReference<ObjectId> latestKnownObjectId;

    @Inject
    GitService(GitProperties config) throws VersioningServiceException {
        this.config = config;
        this.checkoutDirectory = getCheckoutPath();
        try {
            this.gitOperations = new GitOperations(config, checkoutDirectory);

            gitOperations.checkoutBranch(config.getBranch());
            this.latestKnownObjectId = new AtomicReference<>(ObjectId.zeroId());
            LOG.info("Initializing {}, next update = {}", checkoutDirectory, latestKnownObjectId.get());

        } catch (IOException exception) {
            throw new VersioningServiceException("Configuration initialization failed, application can't start", exception);
        }
    }

    private Path getCheckoutPath() {
        final Path configuredFile = config.getLocalRepository();
        if (configuredFile != null) {
            return configuredFile;
        }

        try {
            final Path result = Files.createTempDirectory("config");
            LOG.info("Checking out into a temporary directory: {}", result);
            return result;
        } catch (IOException e) {
            throw new VersioningServiceException(e);
        }
    }

    @Override
    public VersionedConfigUpdate getCurrentState() {
        return new VersionedConfigUpdate(
                checkoutDirectory, Collections.emptySet(), null, latestKnownObjectId.get());
    }

    /**
     * Look and see if the latest SHA on the config directory in the config repo is different to the latest
     * we know about. If it is, grab a reference to an input stream of the mapping file and feed it to the
     * consumer.
     * <p>
     * The first time we are called we should just return an update with all files.
     *
     * @return set of affected files, if any, or empty set
     */
    @Override
    public Optional<VersionedConfigUpdate> checkForUpdate() throws VersioningServiceException {
        if (!gitOperations.pull()) {
            LOG.trace("pull did nothing");
            return empty();
        }

        final ObjectId pulled = gitOperations.getCurrentHead();
        final ObjectId current = latestKnownObjectId.get();
        if (pulled.equals(current)) {
            LOG.trace("head {} didn't change", current);
            return empty();
        }

        final Set<Path> affectedPaths = getAffectedPaths(current, pulled);
        latestKnownObjectId.set(pulled);
        return Optional.of(new VersionedConfigUpdate(
                checkoutDirectory, affectedPaths, current, pulled));
    }

    @Override
    public Set<Path> getAffectedPaths(ObjectId currentHash, ObjectId newHash ) {
        final Set<Path> affectedPaths;
        if (currentHash.equals(ObjectId.zeroId()) || newHash.equals(ObjectId.zeroId())) {
            try {
                affectedPaths = Files.walk(checkoutDirectory)
                        .map(p -> checkoutDirectory.relativize(p))
                        .filter(p -> !p.toString().startsWith(".git"))
                        .collect(Collectors.toSet());
            } catch (IOException e) {
                throw new VersioningServiceException(e);
            }
        } else {
            affectedPaths = gitOperations.affectedFiles(currentHash, newHash)
                    .stream()
                    .map(Paths::get)
                    .collect(Collectors.toSet());
        }
        LOG.info("Update from {} to {} affected paths = {}", currentHash, newHash, affectedPaths);
        return affectedPaths;
    }

    @Override
    public Path getCheckoutDirectory() {
        return checkoutDirectory;
    }

    @Override
    public String getLatestRevision() {
        return latestKnownObjectId.get().getName();
    }

    @Override
    public Optional<ObjectId> getLatestRevisionOid() {
        return Optional.ofNullable(latestKnownObjectId.get());
    }

    public List<URI> getRemoteRepositories() {
        return config.getRemoteRepositories();
    }

    @Override
    public String getBranch() {
        return config.getBranch();
    }

    @Override
    @PreDestroy
    public void close() throws IOException {
        if (config.getLocalRepository() != null) {
            return;
        }

        Files.walkFileTree(checkoutDirectory, DeleteRecursively.INSTANCE);
    }
}
