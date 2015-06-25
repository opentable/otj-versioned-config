package com.opentable.versionedconfig;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.inject.Inject;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.eclipse.jgit.lib.ObjectId;

import com.opentable.logging.Log;

/**
 * Responsible for noticing when service configuration has been updated
 */
class GitService implements VersioningService
{

    private static final Log LOG = Log.findLog();

    private final File checkoutDir;
    private final List<File> configFiles;
    private final Set<String> filesAsGitPaths;

    private final GitOperations gitOperations;

    private AtomicReference<ObjectId> latestKnownObjectId;

    @Inject
    public GitService(VersioningServiceProperties serviceConfig) throws VersioningServiceException
    {
        this.checkoutDir = serviceConfig.localConfigRepository();
        LOG.info("initializing GitService with checkout directory of " + checkoutDir);

        try {
            this.gitOperations = new GitOperations(serviceConfig, checkoutDir);

            gitOperations.checkoutBranch(serviceConfig.configBranch());
            this.latestKnownObjectId = new AtomicReference<>(gitOperations.getCurrentHead());

            final List<String> trimmedFilenames = serviceConfig.configFiles().stream()
                    .map(this::trimLeadingSlash).collect(toList());
            this.configFiles = trimmedFilenames.stream().map(name -> new File(checkoutDir, name)).collect(toList());
            this.filesAsGitPaths = Sets.newHashSet(trimmedFilenames);


        } catch (IOException exception) {
            throw new VersioningServiceException("Configuration initialization failed, application can't start", exception);
        }
    }

    private String trimLeadingSlash(String s) {
        return s.startsWith("/") ? s.substring(1) : s;
    }

    private File getCheckoutDir(URI localRepoUri) throws VersioningServiceException
    {
        try {
            return new File(localRepoUri.toURL().getFile());
        } catch (MalformedURLException e) {
            throw new VersioningServiceException("Malformed local repo path. Should be a local file: URI", e);
        }
    }

    private static final Set<File> NO_AFFECTED_FILES = ImmutableSet.of();

    /**
     * Look and see if the latest SHA on the config directory in the config repo is different to the latest
     * we know about. If it is, grab a reference to an input stream of the mapping file and feed it to the
     * consumer.
     *
     * @param updateConsumer a consumer to process any of the input files for a detected update
     * @return set of affected files, if any, or empty set.
     */
    @Override
    public Set<File> checkForUpdate(Consumer<ConfigUpdate> updateConsumer) throws VersioningServiceException
    {
        if (!gitOperations.pull()) {
            return NO_AFFECTED_FILES;
        }
        final ObjectId latest = gitOperations.getCurrentHead();
        if (latest.equals(latestKnownObjectId.get())) {
            return NO_AFFECTED_FILES;
        }
        final Set<File> affectedFiles = gitOperations.affectedFiles(filesAsGitPaths, latestKnownObjectId.get(), latest)
                .stream()
                .map(this::fileForPath)
                .collect(toSet());
        if (affectedFiles.isEmpty()) {
            LOG.debug("Update " + latest + " doesn't affect any paths I care about");
            return NO_AFFECTED_FILES;
        }
        updateConsumer.accept(new ConfigUpdate(affectedFiles));
        latestKnownObjectId.set(latest);
        return affectedFiles;
    }

    File fileForPath(String path) throws VersioningServiceException {
        return new File(checkoutDir, path);
    }
}
