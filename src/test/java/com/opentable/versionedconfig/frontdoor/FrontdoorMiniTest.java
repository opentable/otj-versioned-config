package com.opentable.versionedconfig.frontdoor;

import java.util.UUID;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;


import com.opentable.versionedconfig.VersioningService;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FrontdoorMiniConfiguration.class)
@TestPropertySource(properties = {
        "OT_ENV=ci-rs",
        "OT_ENV_WHOLE=ci-rs.internal",
        "ot.versioned-config.mike.local=/tmp/gittest/",
        "ot.versioned-config.mike.branch=master",
        "ot.versioned-config.mike.remotes=myremote",
        "ot.versioned-config.mike.remote.myremote.uri=https://f5b27a22883122102a54dc6e8551b93973f47f21:x-oauth-basic@github.com/opentable/service-ot-frontdoor-config"
})
public class FrontdoorMiniTest {
    @Inject
    VersioningService versioningService;
    @Inject
    ConfigUpdateService configUpdateService;

    @Test
    public void testFrontdoor() {
       configUpdateService.tryUpdate();
        Assert.assertTrue(configUpdateService.getLatestValidRevision().isPresent());
    }

}
