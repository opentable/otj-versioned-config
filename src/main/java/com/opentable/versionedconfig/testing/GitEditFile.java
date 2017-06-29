package com.opentable.versionedconfig.testing;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.util.FileUtils;

public class GitEditFile implements GitAction {
    private final Path path;
    private final String contents;
    private final boolean append;

    public GitEditFile(Path path, String contents, boolean append) {
        this.path = path;
        this.contents = contents;
        this.append = append;

        if (this.path.isAbsolute()) {
            throw new IllegalArgumentException("path must not be absolute: " + path);
        }
    }

    @Override
    public void apply(Path root, Git repo) {
        Path target = root.resolve(path);
        try {
            Path basedir = target.getParent();
            if (!root.equals(basedir)) {
                FileUtils.mkdirs(basedir.toFile());
            }

            if (append) {
                Files.write(target, contents.getBytes(Charset.defaultCharset()), CREATE, APPEND);
            } else {
                Files.write(target, contents.getBytes(Charset.defaultCharset()), CREATE, TRUNCATE_EXISTING);
            }

            AddCommand add = repo.add();
            add.addFilepattern(root.relativize(target).toString());
            add.call();
        } catch (IOException | GitAPIException e) {
            throw new RuntimeException("failed to edit file " + target, e);
        }
    }
}
