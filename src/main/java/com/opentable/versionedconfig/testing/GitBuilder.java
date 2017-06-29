package com.opentable.versionedconfig.testing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class GitBuilder {
    private static final String DEFAULT_PREIFX = "test";
    private final List<GitAction> actions = new ArrayList<>();
    private Path localPath;

    public void setLocalPath(Path localPath) {
        this.localPath = localPath;
    }

    public GitBuilder addAction(GitAction action) {
        this.actions.add(action);
        return this;
    }

    public GitBuilder addActions(Collection<GitAction> actions) {
        actions.forEach(this::addAction);
        return this;
    }

    public GitBuilder editFile(Path path, String contents) {
        return addAction(new GitEditFile(path, contents, false));
    }

    public GitBuilder editFile(String path, String contents) {
        return editFile(Paths.get(path), contents);
    }

    public GitBuilder appendFile(Path path, String contents) {
        return addAction(new GitEditFile(path, contents, true));
    }

    public GitBuilder appendFile(String path, String contents) {
        return appendFile(Paths.get(path), contents);
    }

    public GitBuilder commit(String message) {
        return addAction(new GitCommit(message));
    }

    public GitRule rule() {
        try {
            if (localPath == null) {
                localPath = Files.createTempDirectory(DEFAULT_PREIFX);
            }
            return new GitRule(localPath, ImmutableList.copyOf(actions));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
