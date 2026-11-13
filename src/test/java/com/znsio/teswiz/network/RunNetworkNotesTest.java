package com.znsio.teswiz.network;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link RunNetworkNotes}: what happens to the run's network notes when the run finishes. The per-host
 * summary is logged (only when there is something to say), and the notes file is always written into the reports
 * directory, so the pipeline summary can tell "no notes" apart from "this job wrote nothing". Nothing here may fail
 * the run.
 */
class RunNetworkNotesTest {

    private static final NetworkNote SLACK_VIA_SECOND = new NetworkNote("slack", "hooks.slack.com", 443,
            List.of(new NetworkNote.UnreachableAddress("54.0.0.1", "SocketTimeoutException: connect timed out")),
            "54.0.0.2");

    private final NetworkNotes notes = new NetworkNotes();

    @TempDir
    Path reportsDirectory;

    @Test
    void theNotesFile_isWrittenIntoTheReportsDirectory() {
        notes.record(SLACK_VIA_SECOND);

        Path written = RunNetworkNotes.finish(notes, reportsDirectory);

        assertThat(written).isEqualTo(reportsDirectory.resolve(RunNetworkNotes.FILE_NAME));
        assertThat(NetworkNotes.readFrom(written)).containsExactly(SLACK_VIA_SECOND);
    }

    @Test
    void aRunWithoutNotes_stillWritesAnEmptyNotesFile() {
        Path written = RunNetworkNotes.finish(notes, reportsDirectory);

        assertThat(written).exists();
        assertThat(NetworkNotes.readFrom(written)).isEmpty();
    }

    @Test
    void theSummary_isLogged_whenThereAreNotes() {
        notes.record(SLACK_VIA_SECOND);

        try (NetworkLogCapture log = NetworkLogCapture.of(RunNetworkNotes.class)) {
            RunNetworkNotes.finish(notes, reportsDirectory);

            assertThat(log.allMessages()).contains("Network notes for this run: 1 host(s) needed address fallback")
                    .contains("hooks.slack.com:443  fallbacks=1");
        }
    }

    @Test
    void noSummary_isLogged_whenThereAreNoNotes() {
        try (NetworkLogCapture log = NetworkLogCapture.of(RunNetworkNotes.class)) {
            RunNetworkNotes.finish(notes, reportsDirectory);

            assertThat(log.allMessages()).doesNotContain("Network notes for this run");
        }
    }

    @Test
    void aReportsDirectoryThatCannotBeWritten_isLoggedAndNeverThrows() throws Exception {
        Path aFileNotADirectory = Files.createFile(reportsDirectory.resolve("not-a-directory"));

        try (NetworkLogCapture log = NetworkLogCapture.of(RunNetworkNotes.class)) {
            Path written = RunNetworkNotes.finish(notes, aFileNotADirectory);

            assertThat(written).isNull();
            assertThat(log.allMessages()).contains("Could not write the network notes file");
        }
    }
}
