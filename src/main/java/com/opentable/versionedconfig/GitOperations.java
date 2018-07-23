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

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
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
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GitOperations {
    private static final Logger LOG = LoggerFactory.getLogger(GitOperations.class);
    private final Git git;
    private final Optional<CredentialsProvider> credentials;
    private final GitProperties config;

    GitOperations(final GitProperties config, Path checkoutDir) throws VersioningServiceException, IOException {
        this.config = config;
        this.credentials = makeCredentials(config.getUsername(), config.getPassword());
        this.git = openRepo(config, checkoutDir);
    }

    private Optional<CredentialsProvider> makeCredentials(String username, String password) {
        boolean missingUsername = isNullOrEmpty(username);
        boolean missingPassword = isNullOrEmpty(password);

        if (missingUsername && missingPassword) {
            return Optional.empty();
        } else if (missingUsername || missingPassword) {
            throw new IllegalArgumentException("must provide username and password, or both must be empty/null");
        }
        return Optional.of(new UsernamePasswordCredentialsProvider(username, password));
    }

    private Git openRepo(final GitProperties serviceConfig, Path checkoutDir)
            throws VersioningServiceException, IOException {
        if (Files.exists(checkoutDir) && Files.isDirectory(checkoutDir)) {
            LOG.info("checkout directory {} already exists", checkoutDir);
            final boolean dirNotEmpty;
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(checkoutDir)) {
                dirNotEmpty = dirStream.iterator().hasNext();
            }
            if (dirNotEmpty) {
                return new Git(new FileRepository(checkoutDir.resolve(".git").toFile()));
            } else {
                LOG.info("checkout directory {} exists but is empty, can reuse", checkoutDir);
            }
        } else {
            LOG.info("checkout directory {} does not yet exist", checkoutDir);
        }
        final List<URI> remotes = config.getRemoteRepositories();
        final Git result = upstreamRetry(idx -> {
            final String cloneBranch = serviceConfig.getBranch();
            LOG.info("cloning {} (branch {}) to {}", idx, cloneBranch, checkoutDir);

            try {
                CloneCommand clone = Git.cloneRepository()
                        .setBare(false)
                        .setBranch(cloneBranch)
                        .setDirectory(checkoutDir.toFile())
                        .setURI(remotes.get(idx).toString());
                credentials.ifPresent(clone::setCredentialsProvider);
                return clone.call();
            } catch (GitAPIException ioe) {
                throw new VersioningServiceException("Could not clone repo", ioe);
            }
        });
        for (int i = 0; i < remotes.size(); i++) {
            try {
                result.remoteAdd()
                    .setName("remote" + i)
                    .setUri(new URIish(remotes.get(i).toString()))
                    .call();
            } catch (GitAPIException | URISyntaxException e) {
                throw new VersioningServiceException("Could not add remote " + remotes.get(i), e);
            }
        }
        return result;
    }

    boolean pull() throws VersioningServiceException {
        LOG.trace("pulling latest");
        return upstreamRetry(idx -> {
            try {
                final PullCommand pull = git.pull();
                credentials.ifPresent(pull::setCredentialsProvider);
                pull.setRemote("remote" + idx);
                PullResult result = pull.call();
                return result.isSuccessful();
            } catch (GitAPIException e) {
                throw new VersioningServiceException("could not pull", e);
            }
        });
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

    String absoluteLocalPath(URI remoteRepoURI) throws VersioningServiceException {
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
        } catch (IOException | GitAPIException e) {
            throw new VersioningServiceException("specified branch has no commits", e);
        }
    }

    Set<String> affectedFiles(ObjectId oldId, ObjectId newId) throws VersioningServiceException {
        final List<DiffEntry> diffEntries = affectedFilesBetweenCommits(oldId, newId);
        final Set<String> items = diffEntries.stream()
                .map(this::relevantDiffPath)
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(items);
    }

    private String relevantDiffPath(DiffEntry diff) {
        if (DiffEntry.ChangeType.ADD == diff.getChangeType()) {
            return diff.getNewPath();  // there is no old path
        }
        return diff.getOldPath();
    }

    List<DiffEntry> affectedFilesBetweenCommits(ObjectId oldId, ObjectId headId) throws VersioningServiceException {
        final Repository repo = git.getRepository();
        try (RevWalk walk = new RevWalk(repo)) {
            LOG.trace("trying to figure out difference between {} and {}", oldId.toString(), headId.toString());

            final CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
            try (ObjectReader oldReader = repo.newObjectReader()) {
                final RevTree oldTree = walk.parseCommit(oldId).getTree();
                oldTreeParser.reset(oldReader, oldTree.getId());
            }

            final CanonicalTreeParser newTreeParser = new CanonicalTreeParser();
            try (ObjectReader newReader = repo.newObjectReader()) {
                final RevTree newTree = walk.parseCommit(headId).getTree();
                newTreeParser.reset(newReader, newTree.getId());
            }

            return git.diff()
                    .setOldTree(oldTreeParser)
                    .setNewTree(newTreeParser)
                    .call();
        } catch (GitAPIException | IOException e) {
            throw new VersioningServiceException("Can't get diff", e);
        }
    }

    private <T> T upstreamRetry(Function<Integer, T> action) {
        RuntimeException failure = null;
        int idx = -1;
        for (URI remote : config.getRemoteRepositories()) {
            idx += 1;
            try {
                return action.apply(idx);
            } catch (RuntimeException e) {
                LOG.warn("While fetching remote {}", remote, e);
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
        throw new IllegalStateException("no remotes to fetch");
    }
}
