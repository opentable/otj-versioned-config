package com.opentable.versionedconfig;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.opentable.versionedconfig.testing.GitRule;

public class GitServiceTest {
    @Rule
    public TemporaryFolder workFolder = new TemporaryFolder();

    @Rule
    public GitRule remote = GitRule.builder()
            .editFile("foo.txt", "Hello, world")
            .commit("Initial commit")
            .rule();

    @Test
    public void testCloneOnInitialize() throws IOException {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("init");
        final GitProperties gitProperties = getGitProperties(checkoutSpot);
        try (final VersioningService service = new GitService(gitProperties)) {
            assertThat(checkoutSpot.exists()).isTrue();
            assertThat(checkoutSpot.toPath().resolve("foo.txt")).hasContent("Hello, world");

            Path configFile = service.getCurrentState().getBasePath().resolve("foo.txt");
            assertThat(configFile).isRegularFile();
            assertThat(configFile).hasContent("Hello, world");
        }
    }

    @Test
    public void testAffectedFilesExisting() throws IOException {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("init");
        final GitProperties gitProperties = getGitProperties(checkoutSpot);
        try (final VersioningService service = new GitService(gitProperties)) {
            remote.appendFile("foo.txt", "!!!").commit("More config");

            Optional<VersionedConfigUpdate> update = service.checkForUpdate();
            assertThat(update).isPresent();

            VersionedConfigUpdate content = update.get();
            assertThat(content.getChangedFiles()).containsExactly(Paths.get("foo.txt"));
        }
    }

    @Test
    public void testAffectedFilesNew() throws IOException {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("init");
        final GitProperties gitProperties = getGitProperties(checkoutSpot);
        try (final VersioningService service = new GitService(gitProperties)) {
            remote.editFile("bar.txt", "New files rock!").commit("Extra config files");

            Optional<VersionedConfigUpdate> update = service.checkForUpdate();
            assertThat(update).isPresent();

            VersionedConfigUpdate content = update.get();
            assertThat(content.getChangedFiles()).containsExactly(Paths.get("bar.txt"));
        }
    }

    @Test
    public void testClosePreservesLocalClone() throws IOException {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("init");
        final GitProperties gitProperties = getGitProperties(checkoutSpot);
        final VersioningService service = new GitService(gitProperties);

        Path testFile = service.getCurrentState().getBasePath().resolve("foo.txt");

        assertThat(testFile).exists();
        service.close();
        assertThat(testFile).exists();
    }

    @Test
    public void testCloseDestroysTempClone() throws IOException {
        final GitProperties gitProperties = getGitProperties(null);
        final VersioningService service = new GitService(gitProperties);

        Path testFile = service.getCurrentState().getBasePath().resolve("foo.txt");

        assertThat(testFile).exists();
        service.close();
        assertThat(testFile).doesNotExist();
    }

    @Test
    public void testAccessToRemoteRepoAndBranch() throws IOException {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("otpl-deploy");
        final GitProperties gitProperties = getGitProperties(checkoutSpot);
        final VersioningService service = VersioningService.forGitRepository(gitProperties);
        assertThat(service.getBranch()).isEqualTo(gitProperties.getBranch());
        assertThat(service.getRemoteRepository()).isEqualTo(gitProperties.getRemoteRepository());
    }

    private GitProperties getGitProperties(File checkoutSpot) {
        return new GitProperties(remote.getLocalPath().toUri(), null, null, checkoutSpot, "master");
    }
}
