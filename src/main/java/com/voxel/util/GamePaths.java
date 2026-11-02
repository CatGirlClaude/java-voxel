package com.voxel.util;

import java.nio.file.Files;
import java.nio.file.Path;

public final class GamePaths {

    private static final Path RUN_DIR = Path.of("run");

    private GamePaths() {}

    /**
     * Resolves a runtime path relative to the 'run' directory when running from project root,
     * or relative to current working directory if already inside 'run'.
     */
    public static Path resolve(String... subPaths) {
        if (subPaths == null || subPaths.length == 0) {
            return Files.exists(RUN_DIR) ? RUN_DIR : Path.of(".");
        }

        // If 'run' directory exists or we are at project root, route to run/
        if (Files.exists(RUN_DIR) || Files.exists(Path.of("src"))) {
            Path p = RUN_DIR;
            for (String sub : subPaths) {
                p = p.resolve(sub);
            }
            return p;
        }

        Path p = Path.of(subPaths[0]);
        for (int i = 1; i < subPaths.length; i++) {
            p = p.resolve(subPaths[i]);
        }
        return p;
    }
}
