package com.opentable.versionedconfig;

import static com.google.common.collect.ImmutableList.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.opentable.config.Config;
import com.opentable.logging.Log;

/**
 * Make sure we can talk to github to grab config. May require local tweaking to pass.
 * <p/>
 * n.b. for this test to pass, you will need to have the testing.github.auth-key defined to valid auth key
 * which can read the otpl-deply repo. Otherwise the test will go looking for a local copy. Which will fail
 * if you don't have it cloned in the directory it's expecting ($PWD/../otpl-deploy).
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
        final File checkoutSpot = workFolder.newFolder("otpl-deploy");
        final VersioningServiceProperties versioningServiceProperties = getVersioningServiceProperties(checkoutSpot);
        final Consumer<VersionedConfigUpdate> action = mock(Consumer.class);
        VersioningService service = injectorForTest(versioningServiceProperties, action).getInstance(VersioningService.class);
        assertTrue("checkout directory should exist", checkoutSpot.exists());
    }

    @Test
    public void canGetUpdates() throws IOException, VersioningServiceException {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("otpl-deploy");
        final VersioningServiceProperties versioningServiceProperties = getVersioningServiceProperties(checkoutSpot);
        final Consumer<VersionedConfigUpdate> action = mock(Consumer.class);
        VersioningService service = injectorForTest(versioningServiceProperties, action).getInstance(VersioningService.class);
        final boolean[] updateDetected = {false};
        service.checkForUpdate(update -> {
            updateDetected[0] = true;
        });
    }

    @Test
    public void canGetCurrentConfig() throws IOException, VersioningServiceException
    {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("otpl-deploy");
        final VersioningServiceProperties versioningServiceProperties = getVersioningServiceProperties(checkoutSpot);

        Set<File> paths = new HashSet<>();
        final Consumer<VersionedConfigUpdate> action = update -> paths.addAll(update.getAlteredPaths());

        final VersioningService service = injectorForTest(versioningServiceProperties, action).getInstance(VersioningService.class);

        service.checkForUpdate(action);
        assertTrue(!paths.isEmpty());
    }

    @Test
    public void ignoredPathsDontDoAnything() throws IOException, VersioningServiceException, GitAPIException {
        final AtomicBoolean firstConfigRead = new AtomicBoolean(false);
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("otpl-deploy");
        final VersioningServiceProperties versioningServiceProperties = getVersioningServiceProperties(checkoutSpot);

        final Consumer<VersionedConfigUpdate> action = source -> firstConfigRead.set(true);
        final VersioningService service = injectorForTest(versioningServiceProperties, action).getInstance(VersioningService.class);

        blurtRandomRepoChange(checkoutSpot, "someotherfile");

        final AtomicBoolean secondConfigRead = new AtomicBoolean(false);
        final Consumer<VersionedConfigUpdate> action2 = source -> secondConfigRead.set(true);

        service.checkForUpdate(action2);
        assertEquals(true, firstConfigRead.get());
        assertEquals(false, secondConfigRead.get());  // no updates we care about here, sir
    }

    @Test
    public void filesWeCareAboutDontGetIgnored() throws IOException, VersioningServiceException, GitAPIException {
        final AtomicBoolean firstConfigRead = new AtomicBoolean(false);
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("otpl-deploy");
        final VersioningServiceProperties versioningServiceProperties = getVersioningServiceProperties(checkoutSpot);

        final Consumer<VersionedConfigUpdate> action = update -> firstConfigRead.set(true);
        final VersioningService service = injectorForTest(versioningServiceProperties, action).getInstance(VersioningService.class);

        blurtRandomRepoChange(checkoutSpot, "/integrationtest/mappings.cfg.tsv");

        final AtomicBoolean secondConfigRead = new AtomicBoolean(false);
        final Consumer<VersionedConfigUpdate> action2 = update -> secondConfigRead.set(true);

        service.checkForUpdate(action2);
        assertEquals(true, firstConfigRead.get());
        assertEquals(true, secondConfigRead.get());  // we got updated! should have read it again!
    }

    @Test
    public void filesWeCareAboutDontGetIgnoredEvenIfPrefixedWithSlashes() throws IOException, VersioningServiceException, GitAPIException {
        final AtomicBoolean firstConfigRead = new AtomicBoolean(false);
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("otpl-deploy");
        final VersioningServiceProperties versioningServiceProperties = getVersioningServiceProperties(checkoutSpot);
        Mockito.when(versioningServiceProperties.configFiles()).thenReturn(of("/integrationtest/mappings.cfg.tsv"));

        final Consumer<VersionedConfigUpdate> action = stream -> firstConfigRead.set(true);
        final VersioningService service = injectorForTest(versioningServiceProperties, action).getInstance(VersioningService.class);

        blurtRandomRepoChange(checkoutSpot, "/integrationtest/mappings.cfg.tsv");

        final AtomicBoolean secondConfigRead = new AtomicBoolean(false);
        final Consumer<VersionedConfigUpdate> action2 = stream -> secondConfigRead.set(true);

        service.checkForUpdate(action2);
        assertEquals(true, firstConfigRead.get());
        assertEquals(true, secondConfigRead.get());  // we got updated! should have read it again!
    }

    private void blurtRandomRepoChange(File checkoutDir, String filename) throws IOException, GitAPIException {
        final File repoFile = new File(checkoutDir, ".git");
        final Git git = new Git(new FileRepository(repoFile));
        final File touchy = new File(checkoutDir, filename);

        LOG.info("blurting random change into repo " + repoFile);
        LOG.info("creating/verifying " + touchy);

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
        Mockito.when(versioningServiceProperties.configPollingIntervalSeconds()).thenReturn(0L);
        Mockito.when(versioningServiceProperties.repoUsername()).thenReturn(githubAuthKey);
        Mockito.when(versioningServiceProperties.repoPassword()).thenReturn("x-oauth-basic");

        Mockito.when(versioningServiceProperties.localConfigRepository()).thenReturn(checkoutSpot);
        return versioningServiceProperties;
    }

    private Injector injectorForTest(final VersioningServiceProperties versioningServiceProperties, Consumer<VersionedConfigUpdate> action)
    {
        return Guice.createInjector(new AbstractModule()
        {

            @Override
            public void configure()
            {
                install(new VersionedConfigModule());
                //install
                bind(Config.class).toInstance(Config.getEmptyConfig());
                bind(new TypeLiteral<Consumer<VersionedConfigUpdate>>(){}).toInstance(action);
                bind(VersioningServiceProperties.class).toInstance(versioningServiceProperties);
            }
        });
    }
}
