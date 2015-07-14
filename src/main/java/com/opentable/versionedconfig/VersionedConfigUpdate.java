package com.opentable.versionedconfig;

import static java.util.stream.Collectors.toSet;

import java.nio.file.Path;
import java.util.Set;

public final class VersionedConfigUpdate {

    public static class FileInfo {
        private final Path path;
        private final boolean changed;

        public Path getPath() {
            return path;
        }

        public boolean isChanged() {
            return changed;
        }

        private FileInfo(Path path, boolean changed) {
            this.path = path;
            this.changed = changed;
        }
    }

    private final Set<FileInfo> fileInfos;
    private final Set<Path> alteredPaths;
    private final Set<Path> allPaths;

    public VersionedConfigUpdate(Set<Path> alteredPaths, Set<Path> allPaths) {
        this.alteredPaths = alteredPaths;
        this.allPaths = allPaths;
        this.fileInfos = allPaths.stream()
                .map(path -> new FileInfo(path, alteredPaths.contains(path)))
                .collect(toSet());
    }

    public Set<Path> getAlteredPaths() {
        return alteredPaths;
    }

    public Set<Path> getAllPaths() {
        return allPaths;
    }

    public boolean isEmpty() {
        return alteredPaths.isEmpty();
    }
}
