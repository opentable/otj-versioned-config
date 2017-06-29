package com.opentable.versionedconfig.testing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;

import com.opentable.versionedconfig.GitProperties;
import com.opentable.versionedconfig.VersionedConfigUpdate;
import com.opentable.versionedconfig.VersioningService;

public class VersioningServiceIntegrationTest {

    @Rule
    public final GitRule git = GitRule.builder()
            .editFile("foo.txt", "Hello, world!")
            .commit("Initial commit")
            .rule();

    @Test
    public void testVersioningServiceCanClone() {
        GitProperties props = new GitProperties(git.getLocalPath().toUri(), null, null, null, "master");
        VersioningService repo = VersioningService.forGitRepository(props);
        VersionedConfigUpdate currentState = repo.getCurrentState();
        assertThat(currentState.getBasePath().resolve("foo.txt")).isRegularFile();
    }

    @Test
    public void testIsClone() {
        GitProperties props = new GitProperties(git.getLocalPath().toUri(), null, null, null, "master");
        VersioningService repo = VersioningService.forGitRepository(props);
        VersionedConfigUpdate currentState = repo.getCurrentState();

        git.editFile("foo.txt", "Derp derp derp").commit("Additional commit");
        assertThat(currentState.getBasePath().resolve("foo.txt")).hasContent("Hello, world!");
    }

    @Test
    public void testUpdate() {
        GitProperties props = new GitProperties(git.getLocalPath().toUri(), null, null, null, "master");
        VersioningService repo = VersioningService.forGitRepository(props);
        VersionedConfigUpdate currentState = repo.getCurrentState();

        assertThat(currentState.getBasePath().resolve("foo.txt")).hasContent("Hello, world!");
        git.editFile("foo.txt", "Derp derp derp").commit("Additional commit");
        assertThat(repo.checkForUpdate().isPresent()).isTrue();
        currentState = repo.getCurrentState();
        assertThat(currentState.getBasePath().resolve("foo.txt")).hasContent("Derp derp derp");
    }
}
