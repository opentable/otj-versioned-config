package com.opentable.versionedconfig.testing;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.rules.ExternalResource;

public class GitRule extends ExternalResource {
    private final Path localPath;
    private final Collection<GitAction> actions;
    private final Git gitRepo;

    GitRule(Path localPath, Collection<GitAction> actions) {
        this.localPath = localPath;
        this.actions = actions;
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
        InitCommand init = Git.init();
        try {
            return init.setBare(false).setDirectory(this.localPath.toFile()).call();
        } catch (GitAPIException e) {
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
