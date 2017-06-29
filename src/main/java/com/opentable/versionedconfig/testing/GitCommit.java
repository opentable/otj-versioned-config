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
