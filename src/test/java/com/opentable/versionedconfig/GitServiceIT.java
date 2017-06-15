package com.opentable.versionedconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Optional;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Make sure we can talk to github to grab config. May require local tweaking to pass.
 * <p/>
 * n.b. for this test to pass, you will need to have the testing.github.auth-key defined to valid auth key
 * which can read the otpl-deply repo. Otherwise the test will go looking for a local copy. Which will fail
 * if you don't have it cloned in the directory it's expecting ($PWD/../conf).
 */
@Ignore
public class GitServiceIT
{

    private static final Logger LOG = LoggerFactory.getLogger(GitServiceIT.class);
    @Rule
    public TemporaryFolder workFolder = new TemporaryFolder();

    @Test
    public void initializeWillCloneRepo() throws IOException
    {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("init");
        final GitProperties gitProperties = getGitProperties(checkoutSpot);
        try (final VersioningService service = new GitService(gitProperties)) {
            assertThat(checkoutSpot.exists()).isTrue();
        }
    }

    @Test
    public void canGetUpdates() throws IOException, VersioningServiceException {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("update");
        final GitProperties gitProperties = getGitProperties(checkoutSpot);
        try (final VersioningService service = new GitService(gitProperties)) {
            service.checkForUpdate();
        }
    }

    @Test
    public void canGetCurrentConfig() throws IOException, VersioningServiceException
    {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("get");
        final GitProperties gitProperties = getGitProperties(checkoutSpot);

        try (final VersioningService service = new GitService(gitProperties)) {
            final Optional<VersionedConfigUpdate> update = service.checkForUpdate();
            assertThat(update.isPresent()).isFalse();
            // this is right after cloning so of course nothing has changed when we pull
        }
    }

    @Test
    public void ignoredPathsDontDoAnything() throws IOException, VersioningServiceException, GitAPIException {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("otpl-deploy");
        final GitProperties gitProperties = getGitProperties(checkoutSpot);

        try (final VersioningService service = new GitService(gitProperties)) {
            final Optional<VersionedConfigUpdate> firstUpdate = service.checkForUpdate();

            blurtRandomRepoChange(checkoutSpot, "someotherfile");

            final Optional<VersionedConfigUpdate> secondUpdate = service.checkForUpdate();
            assertThat(firstUpdate.isPresent()).isFalse();
            assertThat(secondUpdate.isPresent()).isFalse();  // no updates we care about here, sir
        }
    }

    @Test
    public void filesWeCareAboutDontGetIgnored() throws IOException, VersioningServiceException, GitAPIException {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("otpl-deploy");
        final GitProperties gitProperties = getGitProperties(checkoutSpot);

        try (final VersioningService service = new GitService(gitProperties)) {
            final Optional<VersionedConfigUpdate> firstUpdate = service.checkForUpdate();

            blurtRandomRepoChange(checkoutSpot, "integrationtest/mappings.cfg.tsv");

            final Optional<VersionedConfigUpdate> secondUpdate = service.checkForUpdate();
            assertThat(firstUpdate.isPresent()).isFalse();
            assertThat(secondUpdate.isPresent()).isTrue();
        }
    }

    @Test
    public void setMonitoredFiles_cannotForgetHardWired() throws IOException, VersioningServiceException, GitAPIException {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("otpl-deploy");
        final GitProperties gitProperties = getGitProperties(checkoutSpot);

        try (final VersioningService service = new GitService(gitProperties)) {
            // make a change to a file dont know about. will trigger update
            blurtRandomRepoChange(checkoutSpot, "integrationtest/mappings.cfg.tsv");
            final Optional<VersionedConfigUpdate> firstUpdate = service.checkForUpdate();
            assertThat(firstUpdate.isPresent()).isTrue();

            // make a change to our original file
            blurtRandomRepoChange(checkoutSpot, "integrationtest/mappings.cfg.tsv");

            // it's hardwired, we have an update whether we like it or not
            final Optional<VersionedConfigUpdate> secondUpdate = service.checkForUpdate();
            assertThat(secondUpdate.isPresent()).isTrue();
        }
    }

    @Test
    public void testAccessToRemoteRepoAndBranch() throws IOException {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("otpl-deploy");
        final GitProperties gitProperties = getGitProperties(checkoutSpot);
        final VersioningService service = VersioningService.forGitRepository(gitProperties);
        assertThat(service.getBranch()).isEqualTo(gitProperties.getConfigBranch());
        assertThat(service.getRemoteRepository()).isEqualTo(gitProperties.getRemoteConfigRepository());
    }

    private void blurtRandomRepoChange(File checkoutDir, String filename) throws IOException, GitAPIException {
        final File repoFile = new File(checkoutDir, ".git");
        final Git git = new Git(new FileRepository(repoFile));
        final File touchy = new File(checkoutDir, filename);

        LOG.info("blurting random change into repo '{}'; altering '{}'", repoFile, touchy);

        assertThat(touchy.exists() || touchy.createNewFile()).isTrue();

        try(FileWriter fw = new FileWriter(touchy, true); PrintWriter pw = new PrintWriter(fw)) {
            LOG.info("appending some stuff");
            pw.append("Another line");
            pw.flush();
        }
        git.add().addFilepattern(".").call();
        final RevCommit commit = git.commit().setMessage("how touching").call();
        git.close();
        LOG.info("commit = {}", commit);
    }

    private GitProperties getGitProperties(File checkoutSpot) {
        final URI source;
        final String githubAuthKey = System.getProperty("testing.github.auth-key");
        if (githubAuthKey == null) {
            fail("please supply github auth key");
            return null;
        }
        source = URI.create("https://github.com/opentable/service-ot-frontdoor-config");
        return new GitProperties(
                source, githubAuthKey, "x-oauth-basic", checkoutSpot, "master");
    }
}
