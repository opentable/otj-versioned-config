package com.opentable.versionedconfig;

import static com.google.common.collect.ImmutableList.of;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.opentable.logging.Log;

/**
 * Make sure we can talk to github to grab config. May require local tweaking to pass.
 * <p/>
 * n.b. for this test to pass, you will need to have the testing.github.auth-key defined to valid auth key
 * which can read the otpl-deply repo. Otherwise the test will go looking for a local copy. Which will fail
 * if you don't have it cloned in the directory it's expecting ($PWD/../conf).
 */
public class GitServiceIT
{

    private static final Log LOG = Log.findLog();
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
    public void canGetUpdates() throws IOException, VersioningServiceException {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("update");
        final VersioningServiceProperties versioningServiceProperties = getVersioningServiceProperties(checkoutSpot);
        final VersioningService service = new GitService(versioningServiceProperties);
        service.checkForUpdate();
    }

    @Test
    public void canGetCurrentConfig() throws IOException, VersioningServiceException
    {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("get");
        final VersioningServiceProperties versioningServiceProperties = getVersioningServiceProperties(checkoutSpot);

        final VersioningService service = new GitService(versioningServiceProperties);

        final Optional<VersionedConfigUpdate> update = service.checkForUpdate();
        assertFalse(update.isPresent());
        // this is right after cloning so of course nothing has changed when we pull
    }

    @Test
    public void ignoredPathsDontDoAnything() throws IOException, VersioningServiceException, GitAPIException {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("otpl-deploy");
        final VersioningServiceProperties versioningServiceProperties = getVersioningServiceProperties(checkoutSpot);

        final VersioningService service = new GitService(versioningServiceProperties);
        final Optional<VersionedConfigUpdate> firstUpdate = service.checkForUpdate();

        blurtRandomRepoChange(checkoutSpot, "someotherfile");

        final Optional<VersionedConfigUpdate> secondUpdate = service.checkForUpdate();
        assertFalse(firstUpdate.isPresent());
        assertFalse(secondUpdate.isPresent());  // no updates we care about here, sir
    }

    @Test
    public void filesWeCareAboutDontGetIgnored() throws IOException, VersioningServiceException, GitAPIException {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("otpl-deploy");
        final VersioningServiceProperties versioningServiceProperties = getVersioningServiceProperties(checkoutSpot);

        final VersioningService service = new GitService(versioningServiceProperties);
        final Optional<VersionedConfigUpdate> firstUpdate = service.checkForUpdate();

        blurtRandomRepoChange(checkoutSpot, "/integrationtest/mappings.cfg.tsv");

        final Optional<VersionedConfigUpdate> secondUpdate = service.checkForUpdate();
        assertFalse(firstUpdate.isPresent());
        assertTrue(secondUpdate.isPresent());
    }

    @Test
    public void filesWeCareAboutDontGetIgnoredEvenIfPrefixedWithSlashes() throws IOException, VersioningServiceException, GitAPIException {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("otpl-deploy");
        final VersioningServiceProperties versioningServiceProperties = getVersioningServiceProperties(checkoutSpot);
        final ImmutableList<String> filenamesOfInterest = of("/integrationtest/mappings.cfg.tsv");
        Mockito.when(versioningServiceProperties.configFiles()).thenReturn(filenamesOfInterest);

        final VersioningService service = new GitService(versioningServiceProperties);
        final Optional<VersionedConfigUpdate> firstUpdate = service.checkForUpdate();

        blurtRandomRepoChange(checkoutSpot, "/integrationtest/mappings.cfg.tsv");

        final Optional<VersionedConfigUpdate> secondUpdate = service.checkForUpdate();
        assertFalse(firstUpdate.isPresent());
        assertTrue(secondUpdate.isPresent());
    }

    private void blurtRandomRepoChange(File checkoutDir, String filename) throws IOException, GitAPIException {
        final File repoFile = new File(checkoutDir, ".git");
        final Git git = new Git(new FileRepository(repoFile));
        final File touchy = new File(checkoutDir, filename);

        LOG.info("blurting random change into repo " + repoFile);
        LOG.info("altering file " + touchy);

        assertTrue(touchy.exists() || touchy.createNewFile());

        try(FileWriter fw = new FileWriter(touchy, true); PrintWriter pw = new PrintWriter(fw)) {
            LOG.info("appending some stuff ");
            pw.append("Another line");
            pw.flush();
        }
        git.add().addFilepattern(".").call();
        final RevCommit commit = git.commit().setMessage("how touching").call();
        git.close();
        LOG.info("commit = " + commit);
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
        Mockito.when(versioningServiceProperties.configFiles()).thenReturn(of("integrationtest/mappings.cfg.tsv"));
        Mockito.when(versioningServiceProperties.repoUsername()).thenReturn(githubAuthKey);
        Mockito.when(versioningServiceProperties.repoPassword()).thenReturn("x-oauth-basic");

        Mockito.when(versioningServiceProperties.localConfigRepository()).thenReturn(checkoutSpot);
        return versioningServiceProperties;
    }
}
