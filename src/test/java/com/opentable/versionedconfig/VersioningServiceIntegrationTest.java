package com.opentable.versionedconfig;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;

import com.opentable.versionedconfig.testing.GitRule;

public class VersioningServiceIntegrationTest {

    @Rule
    public final GitRule remote = GitRule.builder()
            .editFile("foo.txt", "Hello, world!")
            .commit("Initial commit")
            .rule();

    @Test
    public void testVersioningServiceCanClone() {
        GitProperties props = new GitProperties(remote.getLocalPath().toUri(), null, null, null, "master");
        VersioningService repo = VersioningService.forGitRepository(props);

        Path testFile = repo.getCurrentState().getBasePath().resolve("foo.txt");

        assertThat(testFile).isRegularFile();
        assertThat(testFile).hasContent("Hello, world!");
    }

    @Test
    public void testIsClone() {
        GitProperties props = new GitProperties(remote.getLocalPath().toUri(), null, null, null, "master");
        VersioningService repo = VersioningService.forGitRepository(props);

        remote.editFile("foo.txt", "Derp derp derp").commit("Additional commit");
        assertThat(repo.getCurrentState().getBasePath().resolve("foo.txt")).hasContent("Hello, world!");
    }

    @Test
    public void testUpdate() {
        GitProperties props = new GitProperties(remote.getLocalPath().toUri(), null, null, null, "master");
        VersioningService repo = VersioningService.forGitRepository(props);

        assertThat(repo.checkForUpdate()).isNotPresent();
        remote.editFile("foo.txt", "Derp derp derp").commit("Additional commit");
        assertThat(repo.checkForUpdate()).isPresent();

        assertThat(repo.getCurrentState().getBasePath().resolve("foo.txt")).hasContent("Derp derp derp");
    }

    @Test
    public void testNewFiles() {
        GitProperties props = new GitProperties(remote.getLocalPath().toUri(), null, null, null, "master");
        VersioningService repo = VersioningService.forGitRepository(props);

        assertThat(repo.checkForUpdate()).isNotPresent();
        remote.editFile("bar.txt", "Derp derp derp").commit("Additional commit");
        assertThat(repo.checkForUpdate()).isPresent();

        Path newFile = repo.getCurrentState().getBasePath().resolve("bar.txt");
        assertThat(newFile).isRegularFile();
        assertThat(newFile).hasContent("Derp derp derp");
    }

    @Test
    public void testSubdirectories() {
        GitProperties props = new GitProperties(remote.getLocalPath().toUri(), null, null, null, "master");
        VersioningService repo = VersioningService.forGitRepository(props);

        assertThat(repo.checkForUpdate()).isNotPresent();
        remote.editFile("nested/bar.txt", "Derp derp derp").commit("Additional commit");
        assertThat(repo.checkForUpdate()).isPresent();

        Path newFile = repo.getCurrentState().getBasePath().resolve("nested/bar.txt");
        assertThat(newFile).isRegularFile();
        assertThat(newFile).hasContent("Derp derp derp");
    }
}
