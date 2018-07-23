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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.Test;

public class VersionedConfigTest {
    private final VersionedConfig config = new VersionedConfig();

    @Test
    public void testOauthToken() {
        GitProperties props = config.defaultVersioningServiceProperties(null, "1234", null, null, null, "master");
        assertThat(props.getUsername()).isEqualTo("1234");
        assertThat(props.getPassword()).isEqualTo(VersionedConfig.X_OAUTH_BASIC);
    }

    @Test
    public void testUsernamePassword() {
        GitProperties props = config.defaultVersioningServiceProperties(null, null, "foo", "bar", null, "master");
        assertThat(props.getUsername()).isEqualTo("foo");
        assertThat(props.getPassword()).isEqualTo("bar");
    }

    @Test
    public void testConflictingCredentials() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> config.defaultVersioningServiceProperties(null, "1234", "foo", "bar", null, "master"))
                .withMessageContaining("oauth-token and username/password are mutually exclusive");
    }
}
