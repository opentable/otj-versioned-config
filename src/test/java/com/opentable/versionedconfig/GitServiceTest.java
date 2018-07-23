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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
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
    public void testUpdates() throws Exception {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("init");
        final GitProperties gitProperties = getGitProperties(checkoutSpot);
        try (final VersioningService service = new GitService(gitProperties)) {
            final Optional<VersionedConfigUpdate> maybeVcu = service.checkForUpdate();
            assertThat(maybeVcu).isPresent();
            final VersionedConfigUpdate vcu = maybeVcu.get();
            assertThat(vcu.getOldRevisionMetadata()).isEqualTo(ObjectId.zeroId());
            assertThat(vcu.getNewRevisionMetadata()).isNotNull();
            assertThat(changeNames(vcu)).contains("foo.txt");
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
            assertThat(content.getChangedFiles()).contains(Paths.get("foo.txt"));
        }
    }

    @Test
    public void testAffectedFilesNew() throws IOException {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("init");
        final GitProperties gitProperties = getGitProperties(checkoutSpot);
        final Path repo = checkoutSpot.toPath();
        try (final VersioningService service = new GitService(gitProperties)) {

            Optional<VersionedConfigUpdate> update = service.checkForUpdate();
            assertThat(update.isPresent());
            assertThat(changeNames(update.get())).contains("foo.txt");

            remote.editFile("bar.txt", "New files rock!").commit("Extra config files");

            update = service.checkForUpdate();
            assertThat(update).isPresent();

            VersionedConfigUpdate content = update.get();
            assertThat(changeNames(content)).containsExactly("bar.txt");
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

    @Test
    public void testRevisionMetadata() throws IOException {
        final GitProperties gitProperties = getGitProperties(null);
        final VersioningService service = new GitService(gitProperties);
        service.checkForUpdate();

        ObjectId currentCommit = remote.getGitRepo().getRepository().resolve(Constants.HEAD);
        assertThat(service.getCurrentState().getOldRevision()).isEqualTo("<unknown>");
        assertThat(service.getCurrentState().getNewRevision()).isEqualTo(currentCommit.getName());

        remote.editFile("foo.txt", "Update the contents!").commit("Fix foo");
        ObjectId nextCommit = remote.getGitRepo().getRepository().resolve(Constants.HEAD);
        Optional<VersionedConfigUpdate> newState = service.checkForUpdate();
        assertThat(newState).isPresent();

        assertThat(newState.get().getOldRevision()).isEqualTo(currentCommit.getName());
        assertThat(newState.get().getNewRevision()).isEqualTo(nextCommit.getName());

        assertThat(newState.get().getOldRevisionMetadata()).isEqualTo(currentCommit);
        assertThat(newState.get().getNewRevisionMetadata()).isEqualTo(nextCommit);
    }

    @Test
    public void testCredentialsMustBothBeEmptyOrNull() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> VersioningService.forGitRepository(
                        new GitProperties(Collections.emptyList(), "username", null, null, "master")))
                .withMessageContaining("must provide username and password");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> VersioningService.forGitRepository(
                        new GitProperties(Collections.emptyList(), null, "password", null, "master")))
                .withMessageContaining("must provide username and password");
    }

    private GitProperties getGitProperties(File checkoutSpot) {
        return new GitProperties(remote.getLocalPath().toUri(), null, null, checkoutSpot, "master");
    }

    private Set<String> changeNames(VersionedConfigUpdate vcu) {
        return vcu.getChangedFiles().stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}
