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
