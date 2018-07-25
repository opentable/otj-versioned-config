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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import com.opentable.logging.CaptureAppender;
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
        final GitProperties gitProperties = getGitProperties(checkoutSpot.toPath());
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
        final GitProperties gitProperties = getGitProperties(checkoutSpot.toPath());
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
        final GitProperties gitProperties = getGitProperties(checkoutSpot.toPath());
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
        final GitProperties gitProperties = getGitProperties(checkoutSpot.toPath());
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
        final GitProperties gitProperties = getGitProperties(checkoutSpot.toPath());
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
    public void testAccessToBranch() throws IOException {
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("otpl-deploy");
        final GitProperties gitProperties = getGitProperties(checkoutSpot.toPath());
        final VersioningService service = VersioningService.forGitRepository(gitProperties);
        assertThat(service.getBranch()).isEqualTo(gitProperties.getBranch());
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
    public void testFallbackPull() throws Exception {
        final CaptureAppender ca = new CaptureAppender();
        ((Logger) LoggerFactory.getLogger(GitOperations.class)).addAppender(ca);
        ca.start();
        workFolder.create();
        final File checkoutSpot = workFolder.newFolder("init");
        final GitProperties gitProperties = new GitProperties(ImmutableList.of(URI.create("git://example.invalid"), remote.getLocalPath().toUri()), checkoutSpot.toPath(), "master");
        try (final VersioningService service = new GitService(gitProperties)) {
            final Optional<VersionedConfigUpdate> maybeVcu = service.checkForUpdate();
            assertThat(maybeVcu).isPresent();
            final VersionedConfigUpdate vcu = maybeVcu.get();
            assertThat(vcu.getOldRevisionMetadata()).isEqualTo(ObjectId.zeroId());
            assertThat(vcu.getNewRevisionMetadata()).isNotNull();
            assertThat(changeNames(vcu)).contains("foo.txt");
        }
        assertThat(ca.captured).anyMatch(e ->
                e.getLevel() == Level.WARN &&
                e.getFormattedMessage().contains("While fetching remote git://example.invalid")
        );
    }

    private GitProperties getGitProperties(Path checkoutSpot) {
        return new GitProperties(remote.getLocalPath().toUri(), checkoutSpot, "master");
    }

    private Set<String> changeNames(VersionedConfigUpdate vcu) {
        return vcu.getChangedFiles().stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}
