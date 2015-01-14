package com.opentable.versionedconfig;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.google.common.base.Preconditions;
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
        this.checkoutDir = getCheckoutDir(serviceConfig.localConfigRepository());
        LOG.info("initializing GitService with checkout directory of " + checkoutDir);

        try {
            final URI localRepoUri = serviceConfig.localConfigRepository();

            Preconditions.checkState("file".equals(localRepoUri.getScheme()), "local repo path must have \"file\" scheme");
            this.gitOperations = new GitOperations(serviceConfig, checkoutDir);

            gitOperations.checkoutBranch(serviceConfig.configBranch());
            this.latestKnownObjectId = new AtomicReference<>(gitOperations.getCurrentHead());

            final String[] split = serviceConfig.configFiles().split(",");
            final List<String> trimmedFilenames = Stream.of(split).map(this::trimLeadingSlash).collect(toList());
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

    @Override
    public void readConfig(Consumer<InputStream> streamTransformer) throws VersioningServiceException
    {
        for (File configFile : configFiles) {
            LOG.info("parsing configuration file " + configFile);
            try (InputStream in = new FileInputStream(configFile)) {
                streamTransformer.accept(in);
            } catch (IOException e) {
                throw new VersioningServiceException(e);
            }
        }
    }

    /**
     * Look and see if the latest SHA on the config directory in the config repo is different to the latest
     * we know about. If it is, grab a reference to an input stream of the mapping file and feed it to the
     * consumer.
     *
     * @param streamTransformer a consumer to process any the input stream opened for a detected update
     * @return true if an update was detected and consumed
     */
    @Override
    public boolean checkForUpdate(Consumer<InputStream> streamTransformer) throws VersioningServiceException
    {
        if (!gitOperations.pull())
            return false;
        final ObjectId latest = gitOperations.getCurrentHead();
        if (latest.equals(latestKnownObjectId.get()))
            return false;

        if (! gitOperations.anyAffectedFiles(filesAsGitPaths, latestKnownObjectId.get(), latest)) {
            LOG.info("Update " + latest + " doesn't affect any paths I care about");
            return false;
        }
        latestKnownObjectId.set(latest);
        readConfig(streamTransformer);
        return true;
    }

    public FileInputStream getFileInputStream(String path) throws VersioningServiceException
    {
        try {
            return new FileInputStream(new File(checkoutDir, path));
        } catch (IOException e) {
            LOG.error(e, "Couldn't load configuration");
            throw new VersioningServiceException(e);
        }
    }

}
