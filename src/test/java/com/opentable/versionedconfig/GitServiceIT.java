package com.opentable.versionedconfig;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.opentable.config.Config;

/**
 * Make sure we can talk to github to grab config. May require local tweaking to pass.
 * <p/>
 * n.b. for this test to pass, you will need to have the testing.github.auth-key defined to valid auth key
 * which can read the otpl-deply repo. Otherwise the test will go looking for a local copy. Which will fail
 * if you don't have it cloned in the directory it's expecting ($PWD/../otpl-deploy).
 */
public class GitServiceIT
{

    @Rule
    public TemporaryFolder workFolder = new TemporaryFolder();

    @Test
    public void initializeWillCloneRepo() throws IOException
    {
        workFolder.create();
        final File checkoutSpot = new File(workFolder.getRoot(), "otpl-deploy");
        final VersioningServiceProperties versioningServiceProperties = versioningServicePropertiesForTest(checkoutSpot);
        ConfigUpdateAction action = mock(ConfigUpdateAction.class);
        VersioningService service = injectorForTest(versioningServiceProperties, action).getInstance(VersioningService.class);
        assertTrue("checkout directory should exist", checkoutSpot.exists());
    }

    @Test
    public void canGetCurrentConfig() throws IOException, VersioningServiceException
    {
        workFolder.create();
        final File checkoutSpot = new File(workFolder.getRoot(), "otpl-deploy");
        final VersioningServiceProperties versioningServiceProperties = versioningServicePropertiesForTest(checkoutSpot);

        final StringBuilder config = new StringBuilder();
        ConfigUpdateAction action = stream -> {
            try {
                config.append(IOUtils.toString(stream));
            } catch (IOException e) {
            }
        };
        VersioningService service = injectorForTest(versioningServiceProperties, action).getInstance(VersioningService.class);

        service.readConfig(action);
        final String configString = config.toString();
        System.out.println("configString=" + configString);
        assertTrue(!configString.isEmpty());
    }


    public VersioningServiceProperties versioningServicePropertiesForTest(File checkoutSpot)
    {
        final URI source;
        final String githubAuthKey = System.getProperty("testing.github.auth-key");
        if (githubAuthKey == null) {
            fail("please supply github auth key");
            return null;
        } else {
            source = URI.create(String.format("https://%s:x-oauth-basic@github.com/opentable/service-ot-frontdoor-config", githubAuthKey));
        }
        final VersioningServiceProperties versioningServiceProperties = mock(VersioningServiceProperties.class);
        Mockito.when(versioningServiceProperties.remoteConfigRepository()).thenReturn(source);
        Mockito.when(versioningServiceProperties.configBranch()).thenReturn("master");
        Mockito.when(versioningServiceProperties.pollingProbePath()).thenReturn("mappings.cfg.tsv");
        Mockito.when(versioningServiceProperties.configFiles()).thenReturn("/mappings.cfg.tsv");
        Mockito.when(versioningServiceProperties.configPollingIntervalSeconds()).thenReturn(0L);

        Mockito.when(versioningServiceProperties.localConfigRepository()).thenReturn(URI.create("file:" + checkoutSpot));
        return versioningServiceProperties;
    }

    private Injector injectorForTest(final VersioningServiceProperties versioningServiceProperties, ConfigUpdateAction action)
    {
        return Guice.createInjector(new AbstractModule()
        {

            @Override
            public void configure()
            {
                install(new VersionedConfigModule());
                //install
                bind(Config.class).toInstance(Config.getEmptyConfig());
                bind(ConfigUpdateAction.class).toInstance(action);
                bind(VersioningServiceProperties.class).toInstance(versioningServiceProperties);
            }
        });
    }
}
