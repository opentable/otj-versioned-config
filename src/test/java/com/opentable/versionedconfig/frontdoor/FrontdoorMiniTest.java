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
package com.opentable.versionedconfig.frontdoor;

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
