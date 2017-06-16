package com.opentable.versionedconfig;


import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.Test;

public class VersioningServicePropertiesTest {
    private final GitProperties props = new GitProperties(
            URI.create("https://github.com/foo/bar"), "jim", "pass", null, "master");

    @Test
    public void testConvertingFromGitProperties() {
        VersioningServiceProperties oldProps = VersioningServiceProperties.fromGitProperties(props);
        assertThat(oldProps.getRemoteConfigRepository()).isEqualTo(props.getRemoteRepository());
        assertThat(oldProps.getRepoUsername()).isEqualTo(props.getUsername());
        assertThat(oldProps.getRepoPassword()).isEqualTo(props.getPassword());
        assertThat(oldProps.getLocalConfigRepository()).isEqualTo(props.getLocalRepository());
        assertThat(oldProps.getConfigBranch()).isEqualTo(props.getBranch());
    }
}
