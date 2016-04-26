package com.opentable.versionedconfig;

import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GitOperations {
    private static final Logger LOG = LoggerFactory.getLogger(GitOperations.class);
    private final Git git;
    private final CredentialsProvider credentials;

    GitOperations(final VersioningServiceProperties serviceConfig, Path checkoutDir) throws VersioningServiceException, IOException {
        this.credentials = new UsernamePasswordCredentialsProvider(serviceConfig.repoUsername(), serviceConfig.repoPassword());
        this.git = openRepo(serviceConfig, checkoutDir);
    }

    private Git openRepo(final VersioningServiceProperties serviceConfig, Path checkoutDir)
            throws VersioningServiceException, IOException {
        if (Files.exists(checkoutDir) && Files.isDirectory(checkoutDir)) {
            LOG.info("checkout directory {} already exists", checkoutDir);
            final boolean dirNotEmpty = Files.newDirectoryStream(checkoutDir).iterator().hasNext();
            if (dirNotEmpty) {
                return new Git(new FileRepository(checkoutDir.resolve(".git").toFile()));
            } else {
                LOG.info("checkout directory {} exists but is empty, can reuse", checkoutDir);
            }
        } else {
            LOG.info("checkout directory {} does not yet exist", checkoutDir);
        }
        final String cloneSource = cloningUriToGitArgument(serviceConfig.remoteConfigRepository());
        final String cloneBranch = serviceConfig.configBranch();
        LOG.info("cloning {} (branch {}) to {}", cloneSource, cloneBranch, checkoutDir);

        try {
            return Git.cloneRepository()
                    .setBare(false)
                    .setCredentialsProvider(credentials)
                    .setBranch(cloneBranch)
                    .setDirectory(checkoutDir.toFile())
                    .setURI(serviceConfig.remoteConfigRepository().toString())
                    .call();
        } catch(GitAPIException ioe) {
            throw new VersioningServiceException("Could not clone repo", ioe);
        }
    }

    boolean pull() throws VersioningServiceException
    {
        LOG.trace("pulling latest");
        try {
            final PullResult result = git.pull().setCredentialsProvider(credentials).call();
            return result.isSuccessful();
        } catch (GitAPIException e) {
            throw new VersioningServiceException("could not pull", e);
        }
    }

    @VisibleForTesting
    void checkoutBranch(String branch) throws VersioningServiceException {
        LOG.info("checking out branch {}", branch);
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
        LOG.trace("getCurrentHead");
        try {
            final ObjectId head = git.getRepository().resolve(Constants.HEAD);
            final Iterable<RevCommit> commits = git.log().add(head).setMaxCount(1).call();
            final Iterator<RevCommit> commIterator = commits.iterator();
            if (commIterator.hasNext()) {
                final ObjectId id = commIterator.next().getId();
                LOG.trace("getCurrentHead got id {}", id);
                return id;
            } else {
                throw new VersioningServiceException("specified branch has no HEAD");
            }
        } catch (IOException|GitAPIException e) {
            throw new VersioningServiceException("specified branch has no commits");
        }
    }

    Set<String> affectedFiles(List<Path> filesRelativeToGitRepo, ObjectId oldId, ObjectId newId) throws VersioningServiceException {
        final List<DiffEntry> diffEntries = affectedFilesBetweenCommits(oldId, newId);
        final Set<String> items = diffEntries.stream()
                .map(this::relevantDiffPath)
 //               .filter(filesRelativeToGitRepo::contains)
                .collect(toSet());
        return ImmutableSet.copyOf(items);
    }

    private String relevantDiffPath(DiffEntry diff) {
        if (DiffEntry.ChangeType.ADD == diff.getChangeType()) {
            return diff.getNewPath();  // there is no old path
        }
        return diff.getOldPath();
    }

    /**
     * What an atrocious API is JGit. All this garbage to get the affected files between
     * two commits. How come I can't just call something like List&lt;File&gt; diffPaths(SHA commit1, SHA commit2)
     * and call it a day? All this is just gross!
     */
    List<DiffEntry> affectedFilesBetweenCommits(ObjectId oldId, ObjectId headId) throws VersioningServiceException {
        try {
            LOG.trace("trying to figure out difference between {} and {}", oldId.toString(), headId.toString());
            final Repository repo = git.getRepository();
            final RevWalk walk = new RevWalk(repo);

            final CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
            final ObjectReader oldReader = repo.newObjectReader();
            final RevTree oldTree = walk.parseCommit(oldId).getTree();
            try {
                oldTreeParser.reset(oldReader, oldTree.getId());
            } finally {
                oldReader.release();
            }

            final CanonicalTreeParser newTreeParser = new CanonicalTreeParser();
            final ObjectReader newReader = repo.newObjectReader();
            final RevTree newTree = walk.parseCommit(headId).getTree();
            try {
                newTreeParser.reset(newReader, newTree.getId());
            } finally {
                newReader.release();
            }

            final List<DiffEntry> diffEntries = git.diff()
                    .setOldTree(oldTreeParser)
                    .setNewTree(newTreeParser)
                    .call();
            return diffEntries;
        } catch (GitAPIException|IOException e) {
            throw new VersioningServiceException("Can't get diff", e);
        }
    }
}
