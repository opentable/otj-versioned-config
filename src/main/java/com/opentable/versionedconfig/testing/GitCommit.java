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

import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

public class GitCommit implements GitAction {
    private final String message;

    public GitCommit(String message) {
        this.message = message;
    }

    @Override
    public void apply(Path root, Git repo) {
        try {
            repo.commit().setMessage(message).call();
        } catch (GitAPIException e) {
            throw new RuntimeException("failed to git-commit", e);
        }
    }
}
