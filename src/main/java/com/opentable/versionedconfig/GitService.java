package com.opentable.versionedconfig;

import static java.util.Optional.empty;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;
import javax.annotation.concurrent.NotThreadSafe;
import javax.inject.Inject;

import org.eclipse.jgit.lib.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opentable.io.DeleteRecursively;

/**
 * Responsible for noticing when service configuration has been updated
 */
@NotThreadSafe
class GitService implements VersioningService {

    private static final Logger LOG = LoggerFactory.getLogger(GitService.class);

    private final Path checkoutDirectory;
    private final GitProperties serviceConfig;

    private final GitOperations gitOperations;

    private AtomicReference<ObjectId> latestKnownObjectId;

    @Inject
    GitService(GitProperties serviceConfig) throws VersioningServiceException {
        this.serviceConfig = serviceConfig;
        this.checkoutDirectory = getCheckoutPath();
        LOG.info("initializing GitService with checkout directory of " + checkoutDirectory);

        try {
            this.gitOperations = new GitOperations(serviceConfig, checkoutDirectory);

            gitOperations.checkoutBranch(serviceConfig.getConfigBranch());
            this.latestKnownObjectId = new AtomicReference<>(gitOperations.getCurrentHead());
            LOG.info("latest SHA = {}", latestKnownObjectId.get());

        } catch (IOException exception) {
            throw new VersioningServiceException("Configuration initialization failed, application can't start", exception);
        }
    }

    private Path getCheckoutPath() {
        final File configuredFile = serviceConfig.getLocalConfigRepository();
        if (configuredFile != null) {
            return configuredFile.toPath();
        }

        Path result;
        try {
            result = Files.createTempDirectory("config");
        } catch (IOException e) {
            throw new VersioningServiceException(e);
        }
        LOG.info("Checking out into a temporary directory: {}", result);
        return result;
    }

    @Override
    public VersionedConfigUpdate getCurrentState() {
        return new VersionedConfigUpdate(
                checkoutDirectory, Stream.empty(), null, latestKnownObjectId.toString());
    }

    /**
     * Look and see if the latest SHA on the config directory in the config repo is different to the latest
     * we know about. If it is, grab a reference to an input stream of the mapping file and feed it to the
     * consumer.
     * <p>
     * The first time we are called we should just return an update with all files.
     *
     * @return set of affected files, if any, or empty set.
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
        LOG.info("newest head = {}", pulled.toString());

        final Set<String> affectedNames = gitOperations.affectedFiles(current, pulled);
        final Stream<Path> affectedPaths = affectedNames
                .stream()
                .map(Paths::get);

        LOG.info("Update from {} to {} affected paths = {}", current, pulled, affectedNames);

        LOG.info("Update {} is relevant to my interests", pulled);
        latestKnownObjectId.set(pulled);
        return Optional.of(new VersionedConfigUpdate(
                checkoutDirectory, affectedPaths, current.toString(), pulled.toString()));
    }

    @Override
    public Path getCheckoutDirectory() {
        return checkoutDirectory;
    }

    @Override
    public String getLatestRevision() {
        return latestKnownObjectId.get().toString();
    }

    @Override
    public URI getRemoteRepository() {
        return serviceConfig.getRemoteRepository();
    }

    @Override
    public String getBranch() {
        return serviceConfig.getConfigBranch();
    }

    @Override
    @PreDestroy
    public void close() throws IOException {
        if (serviceConfig.getLocalConfigRepository() != null) {
            return;
        }

        Files.walkFileTree(checkoutDirectory, DeleteRecursively.INSTANCE);
    }
}
