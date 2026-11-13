package com.znsio.teswiz.network;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link NetworkNotes}: the run-wide record of every connection that needed address fallback. Notes
 * never change a result; they make a firewall or routing problem visible in the log (one line per note and one
 * per-host block at the end of the run), next to the scenario that hit it (listeners), and in the pipeline summary
 * (the notes file every leaf writes and the summary job reads back).
 */
class NetworkNotesTest {

    private static final NetworkNote SLACK_VIA_SECOND = new NetworkNote("slack", "hooks.slack.com", 443,
            List.of(new NetworkNote.UnreachableAddress("54.0.0.1", "SocketTimeoutException: connect timed out")),
            "54.0.0.2");
    private static final NetworkNote WALLET_ALL_DOWN = new NetworkNote("api", "wallet.example", 443,
            List.of(new NetworkNote.UnreachableAddress("10.0.0.1", "ConnectException: Connection refused"),
                    new NetworkNote.UnreachableAddress("10.0.0.2", "ConnectException: Connection refused")),
            null);

    private final NetworkNotes notes = new NetworkNotes();

    @TempDir
    Path tempDir;

    @Test
    void aRecordedNote_isKept() {
        notes.record(SLACK_VIA_SECOND);

        assertThat(notes.notes()).containsExactly(SLACK_VIA_SECOND);
    }

    @Test
    void aNoteThatConnected_describesTheSkippedAddressAndTheOneUsed() {
        assertThat(SLACK_VIA_SECOND.describe()).isEqualTo("hooks.slack.com:443 (slack): 54.0.0.1 unreachable"
                + " (SocketTimeoutException: connect timed out) -> connected via 54.0.0.2");
    }

    @Test
    void aNoteThatNeverConnected_saysEveryAddressWasUnreachable() {
        assertThat(WALLET_ALL_DOWN.connected()).isFalse();
        assertThat(WALLET_ALL_DOWN.describe()).startsWith("wallet.example:443 (api): all 2 address(es) unreachable:")
                .contains("10.0.0.1 (ConnectException: Connection refused)").contains("10.0.0.2");
    }

    @Test
    void recordingANote_logsItAsAWarning() {
        try (NetworkLogCapture log = NetworkLogCapture.of(NetworkNotes.class)) {
            notes.record(SLACK_VIA_SECOND);

            assertThat(log.allMessages()).contains("Network note:").contains("54.0.0.1 unreachable")
                    .contains("connected via 54.0.0.2");
        }
    }

    @Test
    void everyListener_hearsEachNoteAsItIsRecorded() {
        List<NetworkNote> heard = new ArrayList<>();
        notes.onRecord(heard::add);

        notes.record(SLACK_VIA_SECOND);
        notes.record(WALLET_ALL_DOWN);

        assertThat(heard).containsExactly(SLACK_VIA_SECOND, WALLET_ALL_DOWN);
    }

    @Test
    void aListenerThatFails_neverStopsTheNoteBeingRecorded() {
        notes.onRecord(note -> {
            throw new IllegalStateException("listener broke");
        });

        notes.record(SLACK_VIA_SECOND);

        assertThat(notes.notes()).containsExactly(SLACK_VIA_SECOND);
    }

    @Test
    void noNotes_meansAnEmptySummary() {
        assertThat(notes.summary()).isEmpty();
    }

    @Test
    void theSummary_groupsTheNotesByHost_withCountsAndAddresses() {
        notes.record(SLACK_VIA_SECOND);
        notes.record(SLACK_VIA_SECOND);
        notes.record(WALLET_ALL_DOWN);

        String summary = notes.summary();

        assertThat(summary).startsWith("Network notes for this run: 2 host(s) needed address fallback");
        assertThat(summary).contains("hooks.slack.com:443  fallbacks=2  unreachable=[54.0.0.1]  used=[54.0.0.2]")
                .contains("wallet.example:443  fallbacks=1  unreachable=[10.0.0.1, 10.0.0.2]  used=[none]");
    }

    @Test
    void theNotes_writtenToAFile_readBackTheSame() throws IOException {
        notes.record(SLACK_VIA_SECOND);
        notes.record(WALLET_ALL_DOWN);
        Path file = tempDir.resolve("network-notes.json");

        notes.writeTo(file);

        assertThat(NetworkNotes.readFrom(file)).containsExactly(SLACK_VIA_SECOND, WALLET_ALL_DOWN);
    }

    @Test
    void aMissingNotesFile_readsAsNoNotes() {
        assertThat(NetworkNotes.readFrom(tempDir.resolve("absent.json"))).isEmpty();
    }

    @Test
    void clearing_forgetsEveryNote() {
        notes.record(SLACK_VIA_SECOND);

        notes.clear();

        assertThat(notes.notes()).isEmpty();
    }
}
