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
        GitProperties props = new GitProperties(remote.getLocalPath().toUri(), null, "master");
        VersioningService repo = VersioningService.forGitRepository(props);

        Path testFile = repo.getCurrentState().getBasePath().resolve("foo.txt");

        assertThat(testFile).isRegularFile();
        assertThat(testFile).hasContent("Hello, world!");
    }

    @Test
    public void testIsClone() {
        GitProperties props = new GitProperties(remote.getLocalPath().toUri(), null, "master");
        VersioningService repo = VersioningService.forGitRepository(props);

        assertThat(repo.checkForUpdate()).isPresent();
        assertThat(repo.getCurrentState().getBasePath().resolve("foo.txt")).hasContent("Hello, world!");
        remote.editFile("foo.txt", "Derp derp derp").commit("Additional commit");
        assertThat(repo.checkForUpdate()).isPresent();
        assertThat(repo.getCurrentState().getBasePath().resolve("foo.txt")).hasContent("Derp derp derp");
    }

    @Test
    public void testUpdate() {
        GitProperties props = new GitProperties(remote.getLocalPath().toUri(), null, "master");
        VersioningService repo = VersioningService.forGitRepository(props);

        assertThat(repo.checkForUpdate()).isPresent();
        remote.editFile("foo.txt", "Derp derp derp").commit("Additional commit");
        assertThat(repo.checkForUpdate()).isPresent().hasValueSatisfying(
                u -> u.getChangedFiles().toString().contains("foo.txt"));

        assertThat(repo.getCurrentState().getBasePath().resolve("foo.txt")).hasContent("Derp derp derp");
    }

    @Test
    public void testNewFiles() {
        GitProperties props = new GitProperties(remote.getLocalPath().toUri(), null, "master");
        VersioningService repo = VersioningService.forGitRepository(props);

        assertThat(repo.checkForUpdate()).isPresent();
        remote.editFile("bar.txt", "Derp derp derp").commit("Additional commit");
        assertThat(repo.checkForUpdate()).isPresent().hasValueSatisfying(
                u -> u.getChangedFiles().toString().contains("foo.txt"));

        Path newFile = repo.getCurrentState().getBasePath().resolve("bar.txt");
        assertThat(newFile).isRegularFile();
        assertThat(newFile).hasContent("Derp derp derp");
    }

    @Test
    public void testSubdirectories() {
        GitProperties props = new GitProperties(remote.getLocalPath().toUri(), null, "master");
        VersioningService repo = VersioningService.forGitRepository(props);

        remote.editFile("nested/bar.txt", "Derp derp derp").commit("Additional commit");
        assertThat(repo.checkForUpdate()).isPresent();

        Path newFile = repo.getCurrentState().getBasePath().resolve("nested/bar.txt");
        assertThat(newFile).isRegularFile();
        assertThat(newFile).hasContent("Derp derp derp");
    }
}
