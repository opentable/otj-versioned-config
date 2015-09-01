package com.opentable.versionedconfig;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.annotation.concurrent.NotThreadSafe;
import javax.inject.Inject;

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

    /**
     * filenames relative to checkoutDirectory
     */
    private Set<Path> configFileNames;

    /**
     * same thing, absolute
     */
    private Set<Path> configFilePaths;

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
            setMonitoredFiles(serviceConfig.configFiles());
        } catch (IOException exception) {
            throw new VersioningServiceException("Configuration initialization failed, application can't start", exception);
        }
    }

    /**
     * change the set of files we monitor
     */
    @Override
    public void setMonitoredFiles(List<String> paths) {

        final Stream<String> hardwired = serviceConfig.configFiles().stream();
        this.configFileNames = Stream.concat(hardwired, paths.stream())
                .map(this::trimLeadingSlash)
                .map(p -> Paths.get(p))
                .collect(toSet());
        this.configFilePaths = this.configFileNames.stream()
                .map(checkoutDirectory::resolve)
                .collect(toSet());
    }

    @Override
    public VersionedConfigUpdate getInitialState() {

        return new VersionedConfigUpdate(ImmutableSet.copyOf(configFilePaths), copyOf(configFilePaths), latestKnownObjectId.toString());
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

        final Set<String> allAffected = gitOperations.affectedFiles(copyOf(configFileNames), latestKnownObjectId.get(), latest);
        LOG.info("Affected paths = %s ", allAffected.stream().collect(joining(", ")));
        final Set<Path> affectedFiles = allAffected
                .stream()
                .map(Paths::get)
                .filter(configFileNames::contains)
                .map(checkoutDirectory::resolve)
                .collect(toSet());
        final String absolute = affectedFiles.stream().map(Path::toString).collect(joining(", "));
        LOG.info("Affected absolute paths = %s ", absolute);
        final Optional<VersionedConfigUpdate> update;
        if (affectedFiles.isEmpty()) {
            LOG.debug("Update " + latest + " doesn't affect any paths I care about");
            update = empty();
        } else {
            LOG.info("Update " + latest + " is relevant to my interests");
            update = Optional.of(new VersionedConfigUpdate(affectedFiles, copyOf(configFilePaths), latest.toString()));
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

    private String trimLeadingSlash(String s) {
        return s.startsWith("/") ? s.substring(1) : s;
    }
}
