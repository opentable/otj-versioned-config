package com.opentable.versionedconfig;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.opentable.logging.Log;

final class GitOperations {
    private static final Log LOG = Log.findLog();
    private final VersioningServiceProperties config;
    private final File checkoutDir;
    private final Git git;
    private final CredentialsProvider credentials;

    GitOperations(final VersioningServiceProperties serviceConfig, File checkoutDir) throws VersioningServiceException, IOException {
        this.config = serviceConfig;
        this.checkoutDir = checkoutDir;
        this.credentials = new UsernamePasswordCredentialsProvider(serviceConfig.repoUsername(), serviceConfig.repoPassword());
        if (!checkoutDir.exists() || !checkoutDir.isDirectory() || checkoutDir.list().length <= 0) {
            LOG.info("checkout directory " + checkoutDir + " does not exist, cloning");
            final String cloneSource = cloningUriToGitArgument(serviceConfig.remoteConfigRepository());
            LOG.info("cloning " + cloneSource + " to " + checkoutDir);

            if (checkoutDir.mkdirs()) {
                LOG.info("created checkout directory");
            }
            try {
                this.git = Git.cloneRepository()
                        .setBare(false)
                        .setCredentialsProvider(credentials)
                        .setBranch(serviceConfig.configBranch())
                        .setDirectory(checkoutDir)
                        .setURI(serviceConfig.remoteConfigRepository().toString())
                        .call();
            } catch(GitAPIException ioe) {
                throw new VersioningServiceException("Could not clone repo", ioe);
            }
        } else {
            this.git =   new Git(new FileRepository(new File(checkoutDir, ".git")));
        }
    }

    boolean pull() throws VersioningServiceException
    {
        LOG.debug("pulling latest");
        try {
            final PullResult result = git.pull().setCredentialsProvider(credentials).call();
            return result.isSuccessful();
        } catch (GitAPIException e) {
            throw new VersioningServiceException("could not pull", e);
        }
    }

    @VisibleForTesting
    void checkoutBranch(String branch) throws VersioningServiceException {
        LOG.info("checking out branch " + branch);
        try {
            git.checkout().setName(branch).call();
        } catch (GitAPIException cause) {
            throw new VersioningServiceException("Could not check out branch " + branch + " from config repo, please ensure it exists", cause);
        }
    }

    /**
     * Converts source repo URIs to something git can deal with on the command line
     */
    String cloningUriToGitArgument(URI remoteRepoURI) throws VersioningServiceException
    {
        final String scheme = remoteRepoURI.getScheme();
        if ("file".equals(scheme)) {
            return absoluteLocalPath(remoteRepoURI);
        } else {
            return remoteRepoURI.toString();
        }
    }

    String absoluteLocalPath(URI remoteRepoURI) throws VersioningServiceException
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

    ObjectId getCurrentHead() throws VersioningServiceException {
        LOG.debug("getCurrentHead");
        try {
            final ObjectId head = git.getRepository().resolve(Constants.HEAD);
            final Iterable<RevCommit> commits = git.log().add(head).setMaxCount(1).call();
            final Iterator<RevCommit> commIterator = commits.iterator();
            if (commIterator.hasNext()) {
                final ObjectId id = commIterator.next().getId();
                LOG.debug("getCurrentHead got id " + id);
                return id;
            } else {
                throw new VersioningServiceException("specified branch has no HEAD");
            }
        } catch (IOException|GitAPIException e) {
            throw new VersioningServiceException("specified branch has no commits");
        }
    }
}
