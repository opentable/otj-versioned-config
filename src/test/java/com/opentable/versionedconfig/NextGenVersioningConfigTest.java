package com.opentable.versionedconfig;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.env.MockEnvironment;

public class NextGenVersioningConfigTest {

    @Test
    public void testTheFactory() throws Exception {
/*
 ot.versioned.config.$(name).remote.foo.uri=the uri, which may contain auth info, but probably shouldn't
         * ot.versioned.config.$(name).remote.foo.secret= name of CM shared secret
 */
        MockEnvironment mockEnvironment = new MockEnvironment();
        mockEnvironment.setProperty("ot.versioned-config.myconfig.branch", "mybranch");
        mockEnvironment.setProperty("ot.versioned-config.myconfig.local", "mylocal");
        mockEnvironment.setProperty("ot.versioned-config.myconfig.remotes", "firstremote, secondremote, thirdremote, fourthremote");
        // http based, has secret
        mockEnvironment.setProperty("ot.versioned-config.myconfig.remote.firstremote.uri", "http://firstRemote");
        // https based, has secret
        mockEnvironment.setProperty("ot.versioned-config.myconfig.remote.secondremote.uri", "https://secondRemote");
        // https based, has no secret directly or indirectly
        mockEnvironment.setProperty("ot.versioned-config.myconfig.remote.thirdremote.uri", "https://thirdRemote");

        // has secret embedded in its uri
        URI uri = new MutableUri(URI.create("https://fourthRemote")).setPassword("superpassword").setUsername("superuser").toUri();
        mockEnvironment.setProperty("ot.versioned-config.myconfig.remote.fourthremote.uri", uri.toString());
        // This will be ignored, since its not in the list of remotes
        mockEnvironment.setProperty("ot.versioned-config.myconfig.remote.fifthremote.uri", "https://fifthremoteRemote");
        // Cm based paths
        mockEnvironment.setProperty("ot.versioned-config.myconfig.remote.firstremote.secret", "firstsecret");
        mockEnvironment.setProperty("ot.versioned-config.myconfig.remote.secondremote.secret", "secondsecret");
        GitPropertiesFactoryBean factoryBean = new GitPropertiesFactoryBean("myconfig");
        factoryBean.setEnvironment(mockEnvironment);
        factoryBean.setCredentialVersionedURICustomizers(
                Optional.of(Collections.singletonList((secretPath, versionedUri) -> {
                    if (versionedUri.hasPassword() || !secretPath.containsKey(GitPropertiesFactoryBean.PROPERTY_SECRET_PATH)) {
                        return;
                    }
                    String host = versionedUri.getHost();
                    switch (host) {
                        case "firstRemote": {
                            Assert.assertEquals("firstsecret", secretPath.get("secret"));
                            break;
                        }
                        case "secondRemote": {
                            Assert.assertEquals("secondsecret", secretPath.get("secret"));
                            break;
                        }
                        default: {
                            throw new IllegalArgumentException("Nope");
                        }
                    }
                    versionedUri.setPassword("password" + secretPath.get("secret"));
                    versionedUri.setUsername("username" + secretPath.get("secret"));

                })));
        GitProperties gitProperties = factoryBean.getObject();
        Assert.assertEquals(gitProperties.getBranch(), "mybranch");
        Assert.assertEquals(gitProperties.getLocalRepository().toString(), "mylocal");
        List<URI> remoteURIs = gitProperties.getRemoteRepositories();
        Assert.assertEquals(4, remoteURIs.size());
        List<MutableUri> mutableUriList = remoteURIs.stream().map(MutableUri::new).
                sorted(
        ).collect(Collectors.toList());

        MutableUri first = mutableUriList.get(0);
        Assert.assertEquals(first.getHost(), "firstRemote");
        Assert.assertEquals(first.getScheme(), "http");
        Assert.assertTrue(first.hasPassword());
        Assert.assertEquals("usernamefirstsecret", first.getUsername());
        Assert.assertEquals("passwordfirstsecret", first.getPassword());

        MutableUri third = mutableUriList.get(1);
        Assert.assertEquals(third.getHost(), "thirdRemote");
        Assert.assertEquals(third.getScheme(), "https");
        Assert.assertFalse(third.hasPassword());

        MutableUri fourth = mutableUriList.get(2);
        Assert.assertEquals(fourth.getHost(), "fourthRemote");
        Assert.assertEquals(fourth.getScheme(), "https");
        Assert.assertTrue(fourth.hasPassword());
        Assert.assertEquals("superuser", fourth.getUsername());
        Assert.assertEquals("superpassword", fourth.getPassword());

        MutableUri second = mutableUriList.get(3);
        Assert.assertEquals(second.getHost(), "secondRemote");
        Assert.assertEquals(second.getScheme(), "https");
        Assert.assertTrue(second.hasPassword());
        Assert.assertEquals("usernamesecondsecret", second.getUsername());
        Assert.assertEquals("passwordsecondsecret", second.getPassword());
    }
}
