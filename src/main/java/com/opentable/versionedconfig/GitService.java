package com.opentable.versionedconfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.inject.Inject;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import org.apache.commons.io.IOUtils;

import com.opentable.function.IOFunction;
import com.opentable.logging.Log;

/**
 * Responsible for noticing when service configuration has been updated
 */
class GitService implements VersioningService
{

    private static final Log LOG = Log.findLog();

    private final VersioningServiceProperties serviceConfig;
    private final File checkoutDir;
    private final File configFile;

    private final AtomicReference<String> latestKnownSHA;

    @Inject
    public GitService(VersioningServiceProperties serviceConfig) throws VersioningServiceException
    {
        this.serviceConfig = serviceConfig;
        this.checkoutDir = getCheckoutDir(serviceConfig.localConfigRepository());
        LOG.info("initializing GitService with checkout directory of " + checkoutDir);

        try {
            final URI localRepoUri = serviceConfig.localConfigRepository();
            Preconditions.checkState("file".equals(localRepoUri.getScheme()), "local repo path must have \"file\" scheme");
            if (checkoutDir.exists() && checkoutDir.isDirectory() && checkoutDir.list().length > 0) {
                LOG.info("checkout directory " + checkoutDir + " already exists");
            } else {
                LOG.info("checkout directory " + checkoutDir + " does not exist, cloning");
                cloneRepo();
            }
            checkoutBranch(serviceConfig.configBranch());
            this.latestKnownSHA = new AtomicReference<>(getCurrentSHA());
            this.configFile = new File(checkoutDir, serviceConfig.configFile());
        } catch (IOException ioException) {
            throw new VersioningServiceException("Configuration initialization failed, application can't start", ioException);
        }
    }

    private static File getCheckoutDir(URI localRepoUri) throws VersioningServiceException
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
        try (InputStream in = new FileInputStream(configFile)) {
            streamTransformer.accept(in);
        } catch (IOException e) {
            throw new VersioningServiceException(e);
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
        try {
            pull();
            final String currentSHA = getCurrentSHA();
            if (!latestKnownSHA.get().equals(currentSHA)) {
                latestKnownSHA.set(currentSHA);
                LOG.info("new SHA detected, reloading configuration");
                try (InputStream stream = getFileInputStream(serviceConfig.configFile())) {
                    streamTransformer.accept(stream);
                    return true;
                }
            } else {
                LOG.trace("nothing new in config. nothing to do.");
            }
        } catch (IOException e) {
            throw new VersioningServiceException("couldn't check for updates", e);
        }
        return false;
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

    public String getCurrentSHA() throws VersioningServiceException
    {
        try {
            return runProcess("retrieving head SHA", this::stdoutToString, "git", "log", "-n", "1", "--format=\"%H\"",
                    "--no-abbrev-commit", serviceConfig.pollingProbePath())
                    .trim();
        } catch (IOException e) {
            throw new VersioningServiceException("getCurrentSHA failed", e);
        }
    }

    public String pull() throws VersioningServiceException
    {
        try {
            return runProcess("pulling from origin", this::stdoutToString, "git", "pull", "origin", serviceConfig.configBranch());
        } catch (IOException e) {
            throw new VersioningServiceException("pull failed", e);
        }
    }

    @VisibleForTesting
    void cloneRepo() throws VersioningServiceException, IOException
    {
        final URI remoteRepoURI = serviceConfig.remoteConfigRepository();
        final String cloneSource = cloningUriToGitArgument(remoteRepoURI);
        LOG.info("cloning " + cloneSource + " to " + checkoutDir);

        checkoutDir.mkdirs();

        runProcess("clone repo " + remoteRepoURI, this::ignoreOutput, "git", "clone", cloneSource, ".");
    }

    @VisibleForTesting
    void checkoutBranch(String branch) throws IOException
    {
        LOG.info("checking out branch " + branch);
        runProcess("checkout repo branch " + branch, this::ignoreOutput, "git", "checkout", branch);
    }

    /**
     * Converts source repo URIs to something git can deal with on the command line
     */
    public String cloningUriToGitArgument(URI remoteRepoURI) throws VersioningServiceException
    {
        final String scheme = remoteRepoURI.getScheme();
        if ("file".equals(scheme)) {
            return absoluteLocalPath(remoteRepoURI);
        } else {
            return remoteRepoURI.toString();
        }
    }

    public String absoluteLocalPath(URI remoteRepoURI) throws VersioningServiceException
    {
        Preconditions.checkState("file".equals(remoteRepoURI.getScheme()), "Can only deal with file URLs");
        Preconditions.checkState(remoteRepoURI.getHost() == null, "Can only deal with local URLs");
        try {
            final String path = remoteRepoURI.toURL().getPath();
            return new File(path).getCanonicalFile().getPath();
        } catch (IOException e) {
            throw new VersioningServiceException("remote URI format invalid", e);
        }
    }

    private String stdoutToString(Process process) throws IOException
    {
        return IOUtils.toString(process.getInputStream());
    }

    private boolean ignoreOutput(Process process) throws IOException
    {
        return true;
    }

    private <T> T runProcess(String description, IOFunction<Process, T> outputHandler, String... args) throws IOException
    {
        final ProcessBuilder processBuilder = new ProcessBuilder(args[0]).command(args).directory(checkoutDir);
        Process process = null;
        try {
            process = processBuilder.start();
            if (process.waitFor() == 0) {
                return outputHandler.apply(process);
            }
            throw new IOException("Couldn't " + description + ": " + IOUtils.toString(process.getErrorStream()));
        } catch (InterruptedException e ) {
            throw new IOException(description + " was interrupted", e);
        } finally {
            makeSureProcessIsDead(process, description);
        }
    }

    /**
     * make sure a process is really dead
     */
    private void makeSureProcessIsDead(Process process, String description)
    {
        if (process == null) {
            return;
        }
        try {
            if (process.waitFor(3000, TimeUnit.MILLISECONDS)) {
                return; // process is gone already, we're OK.
            }
            LOG.error("Destroying process forcibly: " + description);
            process.destroyForcibly();
        } catch (InterruptedException e1) {
            // nothing we can do here
        }
    }
}
