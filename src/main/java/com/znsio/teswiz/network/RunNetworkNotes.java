package com.znsio.teswiz.network;

import java.io.IOException;
import java.nio.file.Path;

/**
 * What happens to a run's network notes when the run finishes: the per-host summary is logged as a warning (only when
 * there are notes), and the notes are written to {@value #FILE_NAME} in the reports directory — always, even when
 * empty, so a pipeline summary can tell "no notes" apart from "this job wrote nothing". Never throws: a note is never
 * worth failing a run for.
 */
public final class RunNetworkNotes {

    /** The name of the notes file written into the reports directory. */
    public static final String FILE_NAME = "network-notes.json";

    // LogManager/Logger are fully-qualified ON PURPOSE (same as JUnitResults): an auto-import-organizer in this
    // environment rewrites a plain log4j import to java.util.logging.LogManager and drops Logger, breaking the build.
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager
            .getLogger(RunNetworkNotes.class.getName());

    private RunNetworkNotes() {
    }

    /**
     * @param notes            the run's notes
     * @param reportsDirectory where the notes file goes
     * @return the notes file written, or {@code null} when it could not be written (the reason is logged)
     */
    public static Path finish(NetworkNotes notes, Path reportsDirectory) {
        String summary = notes.summary();
        if (!summary.isEmpty()) {
            LOGGER.warn("{}", summary);
        }
        Path file = reportsDirectory.resolve(FILE_NAME);
        try {
            notes.writeTo(file);
            return file;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not write the network notes file {}: {}", file, FailureDetail.describe(e));
            return null;
        }
    }
}
