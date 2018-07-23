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
package com.opentable.versionedconfig.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Rule;
import org.junit.Test;

public class GitRuleTest {
    @Rule
    public final GitRule git = GitRule.builder()
            .editFile("foo.txt", "Hello, world")
            .commit("Initial commit")
            .rule();

    @Rule
    public final GitRule gitComplex = GitRule.builder()
            .editFile("foo.txt", "Hello, world")
            .commit("Initial commit")
            .appendFile("foo.txt", "!")
            .commit("Forgot exclamation")
            .editFile("bar.txt", "Neat!")
            .commit("Very important information")
            .rule();

    @Test
    public void testCreateFileWithContents() {
        assertThat(git.getLocalPath().resolve("foo.txt")).isRegularFile();
        assertThat(git.getLocalPath().resolve("foo.txt")).hasContent("Hello, world");
    }

    @Test
    public void testBeCleanAfterCommit() throws GitAPIException {
        assertThat(git.getGitRepo().status().call().isClean()).isTrue();
    }

    @Test
    public void testUpdateContentInPlace() {
        git.editFile("foo.txt", "Derp derp derp").commit("Derp");
        assertThat(git.getLocalPath().resolve("foo.txt")).hasContent("Derp derp derp");
    }

    @Test
    public void testAppendContent() {
        git.appendFile("foo.txt", "Derp derp derp").commit("Append derp");
        assertThat(git.getLocalPath().resolve("foo.txt")).hasContent("Hello, worldDerp derp derp");
    }

    @Test
    public void testCommitMessage() throws GitAPIException {
        assertThat(git.getGitRepo().log().call().iterator().next().getFullMessage()).isEqualTo("Initial commit");
    }

    @Test
    public void testSubdirectory() throws GitAPIException {
        git.editFile("nested/config.txt", "I am nested!").commit("Add subdir");
        assertThat(git.getGitRepo().status().call().isClean()).isTrue();
        assertThat(git.getLocalPath().resolve("nested")).isDirectory();
        assertThat(git.getLocalPath().resolve("nested/config.txt")).isRegularFile();
        assertThat(git.getLocalPath().resolve("nested/config.txt")).hasContent("I am nested!");
    }

    @Test
    public void testFailOnAbsolute() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> git.editFile("/nested/config.txt", "Fail"))
                .withMessageContaining("path must not be absolute");
    }

    @Test
    public void testComplexInitialize() throws GitAPIException {
        assertThat(gitComplex.getLocalPath().resolve("foo.txt")).isRegularFile();
        assertThat(gitComplex.getLocalPath().resolve("foo.txt")).hasContent("Hello, world!");
        assertThat(gitComplex.getLocalPath().resolve("bar.txt")).isRegularFile();
        assertThat(gitComplex.getLocalPath().resolve("bar.txt")).hasContent("Neat!");
        assertThat(gitComplex.getGitRepo().status().call().isClean()).isTrue();
        assertThat(gitComplex.getGitRepo().log().call()).hasSize(3);
    }
}
