package com.opentable.versionedconfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Make sure we can talk to github to grab config. May require local tweaking to pass.
 * <p/>
 * n.b. for this test to pass, you will need to have the testing.github.auth-key defined to valid auth key
 * which can read the otpl-deply repo. Otherwise the test will go looking for a local copy. Which will fail
 * if you don't have it cloned in the directory it's expecting ($PWD/../conf).
 */
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
        final VersioningServiceProperties versioningServiceProperties = getVersioningServiceProperties(checkoutSpot);
        final VersioningService service = new GitService(versioningServiceProperties);
        assertTrue("checkout directory should exist", checkoutSpot.exists());
    }

    @Test
    public void noUpdatesToBeHadInitially() throws IOException, VersioningServiceException {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("update");
        final VersioningServiceProperties versioningServiceProperties = getVersioningServiceProperties(checkoutSpot);
        final VersioningService service = new GitService(versioningServiceProperties);
        assertFalse(service.checkForUpdate().isPresent());
    }

    @Test
    public void simpleUpdate() throws IOException, VersioningServiceException, GitAPIException {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("otpl-deploy");
        final VersioningServiceProperties versioningServiceProperties = getVersioningServiceProperties(checkoutSpot);

        final VersioningService service = new GitService(versioningServiceProperties);

        final String filename = "integrationtest-local/mappings.cfg.tsv";
        blurtRandomRepoChange(checkoutSpot, filename);

        final Optional<VersionedConfigUpdate> update = service.checkForUpdate();

        final Set<Path> expectedChangedPaths = ImmutableSet.of(Paths.get(filename));
        assertEquals(expectedChangedPaths, update.get().getChangedFiles());
    }

    @Test
    public void twoUpdates() throws IOException, VersioningServiceException, GitAPIException {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("otpl-deploy");
        final VersioningServiceProperties versioningServiceProperties = getVersioningServiceProperties(checkoutSpot);

        final VersioningService service = new GitService(versioningServiceProperties);

        final String filename = "integrationtest-local/mappings.cfg.tsv";
        blurtRandomRepoChange(checkoutSpot, filename);

        final String filename2 = "integrationtest-local/mappings2.cfg.tsv";
        blurtRandomRepoChange(checkoutSpot, filename2);

        final Optional<VersionedConfigUpdate> update = service.checkForUpdate();

        final Set<Path> expectedChangedPaths = ImmutableSet.of(Paths.get(filename), Paths.get(filename2));
        assertEquals(expectedChangedPaths, update.get().getChangedFiles());
    }

    @Test
    public void addNewFileCountsAsChange() throws IOException, VersioningServiceException, GitAPIException {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("otpl-deploy");
        final VersioningServiceProperties versioningServiceProperties = getVersioningServiceProperties(checkoutSpot);

        final VersioningService service = new GitService(versioningServiceProperties);

        // we don't know about this file, so adding it will result in an update
        final String filename = "integrationtest-local/things.txt";
        blurtRandomRepoChange(checkoutSpot, filename);
        final Optional<VersionedConfigUpdate> update = service.checkForUpdate();
        final Set<Path> alteredPaths = update.get().getChangedFiles();
        assertEquals(checkoutSpot.toPath(), update.get().getBasePath());
        final ImmutableSet<Path> expected = ImmutableSet.of(Paths.get(filename));
        assertEquals(expected, alteredPaths);
    }


    private void blurtRandomRepoChange(File checkoutDir, String filename) throws IOException, GitAPIException {
        final File repoFile = new File(checkoutDir, ".git");
        final Git git = new Git(new FileRepository(repoFile));
        final File touchy = new File(checkoutDir, filename);

        LOG.info("blurting random change into repo '%s'; altering '%s'", repoFile, touchy);

        boolean exists = touchy.exists();
        if (!exists) {
            exists = touchy.createNewFile();
        }
        assertTrue(exists);

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

    private VersioningServiceProperties getVersioningServiceProperties(File checkoutSpot) {
        final URI source;
        final String githubAuthKey = System.getProperty("testing.github.auth-key");
        if (githubAuthKey == null) {
            fail("please supply github auth key");
            return null;
        }
        source = URI.create("https://github.com/opentable/service-ot-frontdoor-config");
        final VersioningServiceProperties versioningServiceProperties = mock(VersioningServiceProperties.class);
        Mockito.when(versioningServiceProperties.remoteConfigRepository()).thenReturn(source);
        Mockito.when(versioningServiceProperties.configBranch()).thenReturn("master");
        Mockito.when(versioningServiceProperties.repoUsername()).thenReturn(githubAuthKey);
        Mockito.when(versioningServiceProperties.repoPassword()).thenReturn("x-oauth-basic");

        Mockito.when(versioningServiceProperties.localConfigRepository()).thenReturn(checkoutSpot);
        return versioningServiceProperties;
    }
}
