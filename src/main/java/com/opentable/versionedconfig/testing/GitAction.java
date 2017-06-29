package com.opentable.versionedconfig.testing;

import java.nio.file.Path;

import org.eclipse.jgit.api.Git;

public interface GitAction {
    void apply(Path root, Git repo);
}
