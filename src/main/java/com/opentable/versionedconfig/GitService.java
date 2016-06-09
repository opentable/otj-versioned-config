package com.opentable.versionedconfig;

import static com.google.common.collect.ImmutableSet.copyOf;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.annotation.concurrent.NotThreadSafe;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.eclipse.jgit.lib.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opentable.io.DeleteRecursively;
import com.opentable.lifecycle.LifecycleStage;
import com.opentable.lifecycle.guice.OnStage;
/**
 * Responsible for noticing when service configuration has been updated
 */
@NotThreadSafe
@SuppressWarnings("PMD.TooManyStaticImports")
class GitService implements VersioningService
{

    private static final Logger LOG = LoggerFactory.getLogger(GitService.class);

    private final Path checkoutDirectory;
    private final VersioningServiceProperties serviceConfig;

    /**
     * initial paths to start watching
     */
    private final List<Path> initialPaths;

    /**
     * Filenames relative to checkoutDirectory which we want to keep an eye on.
     *
     * Includes initialPaths.
     */
    private Set<Path> allMonitoredPaths;

    private final GitOperations gitOperations;

    private AtomicReference<ObjectId> latestKnownObjectId;

    @Inject
    GitService(VersioningServiceProperties serviceConfig) throws VersioningServiceException
    {
        this.checkoutDirectory = getCheckoutPath(serviceConfig);
        LOG.info("initializing GitService with checkout directory of " + checkoutDirectory);

        try {
            this.gitOperations = new GitOperations(serviceConfig, checkoutDirectory);

            gitOperations.checkoutBranch(serviceConfig.configBranch());
            this.latestKnownObjectId = new AtomicReference<>(gitOperations.getCurrentHead());
            LOG.info("latest SHA = {}", latestKnownObjectId.get());

            this.serviceConfig = serviceConfig;
            this.initialPaths = serviceConfig.configFiles().stream()
                    .map(this::cleanPath)
                    .map(Paths::get)
                    .collect(toList());
            LOG.info("hardwired paths = {}", initialPaths.stream().map(Path::toString).collect(joining(", ")));
            this.allMonitoredPaths = new HashSet<>(initialPaths); // initially
        } catch (IOException exception) {
            throw new VersioningServiceException("Configuration initialization failed, application can't start", exception);
        }
    }

    private Path getCheckoutPath(VersioningServiceProperties serviceConfig) {
        final File configuredFile = serviceConfig.localConfigRepository();
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

    /**
     * change the set of files we monitor. hardwired ones never go away.
     */
    @Override
    public void setAllMonitoredPaths(Set<Path> paths) {
        final Stream<Path> stream = paths.stream()
                .map(Object::toString)
                .map(this::cleanPath)
                .map(Paths::get);
        this.allMonitoredPaths = Stream.concat(initialPaths.stream(), stream).collect(toSet());
        LOG.debug("setAllMonitoredPaths: {}", allMonitoredPaths.stream().map(Path::toString).collect(joining(", ")));
    }

    @Override
    public VersionedConfigUpdate getInitialState() {
        return new VersionedConfigUpdate(
                checkoutDirectory, copyOf(initialPaths), copyOf(initialPaths), latestKnownObjectId.toString()
        );
    }

    @Override
    public VersionedConfigUpdate getCurrentState() {
        return new VersionedConfigUpdate(
                checkoutDirectory, ImmutableSet.of(), copyOf(allMonitoredPaths), latestKnownObjectId.toString());
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
    public Optional<VersionedConfigUpdate> checkForUpdate() throws VersioningServiceException
    {
        if (!gitOperations.pull()) {
            LOG.debug("pull did nothing");
            return empty();
        }
        final ObjectId latest = gitOperations.getCurrentHead();
        if (latest.equals(latestKnownObjectId.get())) {
            LOG.trace("SHA didn't change");
            return empty();
        }
        LOG.info("newest SHA = {}", latest.toString());

        final Set<String> allAffected = gitOperations.affectedFiles(
                ImmutableList.copyOf(allMonitoredPaths), latestKnownObjectId.get(), latest);
        LOG.info("Affected paths = {}", allAffected.stream().collect(joining(", ")));
        final Set<Path> affectedFiles = allAffected
                .stream()
                .map(Paths::get)
                .filter(allMonitoredPaths::contains)
                .collect(toSet());
        final String absolute = affectedFiles.stream().map(Path::toString).collect(joining(", "));
        LOG.info("Affected absolute paths = {}", absolute);
        final Optional<VersionedConfigUpdate> update;
        if (affectedFiles.isEmpty()) {
            LOG.debug("Update {} doesn't affect any paths I care about", latest);
            update = empty();
        } else {
            LOG.info("Update {} is relevant to my interests", latest);
            update = Optional.of(new VersionedConfigUpdate(
                    checkoutDirectory, affectedFiles, copyOf(allMonitoredPaths), latest.toString())
            );
        }
        latestKnownObjectId.set(latest);
        return update;
    }

    @Override
    public Path getCheckoutDirectory() {
        return checkoutDirectory;
    }

    @Override
    public String getLatestRevision() {
        return latestKnownObjectId.get().toString();
    }

    public List<Path> getInitialPaths() {
        return initialPaths;
    }

    private String cleanPath(String path) {
        String trimmed = path.trim();
        if (trimmed.startsWith("/")) {
            return trimmed.substring(1);
        }
        return trimmed;
    }

    @Override
    @OnStage(LifecycleStage.STOP)
    public void close() throws IOException {
        if (serviceConfig.localConfigRepository() != null) {
            return;
        }

        Files.walkFileTree(checkoutDirectory, DeleteRecursively.INSTANCE);
    }
}
