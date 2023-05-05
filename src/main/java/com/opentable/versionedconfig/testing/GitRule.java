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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.util.SystemReader;
import org.junit.rules.ExternalResource;

public class GitRule extends ExternalResource {
    private final Path localPath;
    private final Collection<GitAction> actions;
    private final Git gitRepo;
    private final boolean readUserConfig;

    GitRule(Path localPath, Collection<GitAction> actions) {
        this.localPath = localPath;
        this.actions = actions;
        this.readUserConfig = false;
        this.gitRepo = initRepo();

    }

    GitRule(Path localPath, Collection<GitAction> actions, boolean readUserConfig) {
        this.localPath = localPath;
        this.actions = actions;
        this.readUserConfig = readUserConfig;
        this.gitRepo = initRepo();
    }

    public static GitBuilder builder() {
        return new GitBuilder();
    }

    public GitRule editFile(Path path, String contents) {
        return runImmediate(new GitEditFile(path, contents, false));
    }

    public GitRule editFile(String path, String contents) {
        return editFile(Paths.get(path), contents);
    }

    public GitRule appendFile(Path path, String contents) {
        return runImmediate(new GitEditFile(path, contents, true));
    }

    public GitRule appendFile(String path, String contents) {
        return appendFile(Paths.get(path), contents);
    }

    public GitRule commit(String message) {
        return runImmediate(new GitCommit(message));
    }

    private GitRule runImmediate(GitAction action) {
        action.apply(localPath, gitRepo);
        return this;
    }

    private Git initRepo() {
        try {
            if (!readUserConfig) {
                SystemReader.getInstance().getUserConfig().clear();
            }
            SystemReader.getInstance().getUserConfig().setBoolean(ConfigConstants.CONFIG_GC_SECTION, null, ConfigConstants.CONFIG_KEY_AUTODETACH, false);
            InitCommand init = Git.init();
            return init.setBare(false).setDirectory(this.localPath.toFile()).call();
        } catch (GitAPIException | ConfigInvalidException | IOException e) {
            throw new RuntimeException("failed to initialize temporary repo", e);
        }
    }

    public Path getLocalPath() {
        return localPath;
    }

    public Git getGitRepo() {
        return gitRepo;
    }

    @Override
    protected void before() {
        actions.forEach(this::runImmediate);
    }

    @Override
    protected void after() {
        gitRepo.close();
    }
}
