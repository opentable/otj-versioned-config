package com.opentable.versionedconfig;

import static com.google.common.collect.ImmutableSet.copyOf;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.annotation.concurrent.NotThreadSafe;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.eclipse.jgit.lib.ObjectId;

import com.opentable.logging.Log;

/**
 * Responsible for noticing when service configuration has been updated
 */
@NotThreadSafe
class GitService implements VersioningService
{

    private static final Log LOG = Log.findLog();

    private final Path checkoutDirectory;
    private final VersioningServiceProperties serviceConfig;
    private final Set<Path> hardwiredPaths;

    /**
     * filenames relative to checkoutDirectory which we want to keep an eye on
     */
    private Set<Path> monitoredFiles;

    private final GitOperations gitOperations;

    private AtomicReference<ObjectId> latestKnownObjectId;

    @Inject
    public GitService(VersioningServiceProperties serviceConfig) throws VersioningServiceException
    {
        this.checkoutDirectory = serviceConfig.localConfigRepository().toPath();
        LOG.info("initializing GitService with checkout directory of " + checkoutDirectory);

        try {
            this.gitOperations = new GitOperations(serviceConfig, checkoutDirectory);

            gitOperations.checkoutBranch(serviceConfig.configBranch());
            this.latestKnownObjectId = new AtomicReference<>(gitOperations.getCurrentHead());
            LOG.info("latest SHA = %s ", latestKnownObjectId.get());

            this.serviceConfig = serviceConfig;
            this.hardwiredPaths = serviceConfig.configFiles().stream()
                    .map(this::cleanPath)
                    .map(Paths::get)
                    .collect(toSet());
            LOG.info("hardwired paths = %s", hardwiredPaths.stream().map(Path::toString).collect(joining(", ")));
            this.monitoredFiles = hardwiredPaths; // initially
        } catch (IOException exception) {
            throw new VersioningServiceException("Configuration initialization failed, application can't start", exception);
        }
    }

    /**
     * change the set of files we monitor. hardwired ones never go away.
     */
    @Override
    public void setMonitoredFiles(Set<Path> paths) {
        final Stream<Path> stream = paths.stream()
                .map(Object::toString)
                .map(this::cleanPath)
                .map(Paths::get);
        this.monitoredFiles = Stream.concat(hardwiredPaths.stream(), stream).collect(toSet());
        LOG.debug("setMonitoredFiles: %s", monitoredFiles.stream().map(Path::toString).collect(joining(", ")));
    }

    @Override
    public VersionedConfigUpdate getInitialState() {
        return new VersionedConfigUpdate(
                checkoutDirectory, copyOf(hardwiredPaths), copyOf(hardwiredPaths), latestKnownObjectId.toString()
        );
    }

    @Override
    public VersionedConfigUpdate getCurrentState() {
        return new VersionedConfigUpdate(
                checkoutDirectory, ImmutableSet.of(), copyOf(monitoredFiles), latestKnownObjectId.toString());
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
            LOG.debug("SHA didn't change");
            return empty();
        }
        LOG.info("newest SHA = %s ", latest.toString());

        final Set<String> allAffected = gitOperations.affectedFiles(
                ImmutableList.copyOf(monitoredFiles), latestKnownObjectId.get(), latest);
        LOG.info("Affected paths = %s ", allAffected.stream().collect(joining(", ")));
        final Set<Path> affectedFiles = allAffected
                .stream()
                .map(Paths::get)
                .filter(monitoredFiles::contains)
                .collect(toSet());
        final String absolute = affectedFiles.stream().map(Path::toString).collect(joining(", "));
        LOG.info("Affected absolute paths = %s ", absolute);
        final Optional<VersionedConfigUpdate> update;
        if (affectedFiles.isEmpty()) {
            LOG.debug("Update " + latest + " doesn't affect any paths I care about");
            update = empty();
        } else {
            LOG.info("Update " + latest + " is relevant to my interests");
            update = Optional.of(new VersionedConfigUpdate(
                    checkoutDirectory, affectedFiles, copyOf(monitoredFiles), latest.toString())
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

    private String cleanPath(String path) {
        String trimmed = path.trim();
        if (trimmed.startsWith("/")) {
            return trimmed.substring(1);
        }
        return trimmed;
    }
}
