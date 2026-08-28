package com.znsio.teswiz.filters.apitraffic;

import java.nio.file.Path;

/**
 * Resolves the directory for the currently running scenario, into which the
 * api-traffic files for that scenario are written.
 * <p>
 * Implemented as a functional interface so production code can read teswiz's
 * per-scenario report folder while unit tests supply a temporary directory.
 */
@FunctionalInterface
public interface ScenarioDirectoryResolver {

    /**
     * Returns the directory for the current scenario. The recorder writes its
     * {@code api-traffic} sub-folder inside this directory.
     */
    Path resolve();
}
