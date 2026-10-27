package com.znsio.teswiz.reporting.notify;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;


/**
 * Unit tests for {@link BranchGate}.
 * <p>
 * The gate decides whether notifications are allowed for the current branch against a configured allowlist (exact
 * names plus glob patterns), and reports allowlist entries that no longer exist as remote branches (a warning, never a
 * failure). The remote-branch set is injected so these tests do not touch the network.
 */
class BranchGateTest {

    @Test
    void masterIsAllowedByTheDefaultAllowlist() {
        BranchGate gate = new BranchGate(List.of("master"));

        assertThat(gate.allows("master")).isTrue();
    }

    @Test
    void aBranchNotOnTheAllowlistIsRejected() {
        BranchGate gate = new BranchGate(List.of("master"));

        assertThat(gate.allows("F_GAMC-19572_notifications")).isFalse();
    }

    @Test
    void anExactNonMasterEntryIsAllowed() {
        BranchGate gate = new BranchGate(List.of("master", "F_GAMC-19572_notifications"));

        assertThat(gate.allows("F_GAMC-19572_notifications")).isTrue();
    }

    @Test
    void aGlobPatternMatchesByPrefix() {
        BranchGate gate = new BranchGate(List.of("release/*"));

        assertThat(gate.allows("release/2026.10")).isTrue();
        assertThat(gate.allows("feature/x")).isFalse();
    }

    @Test
    void staleLiteralEntryWithNoRemoteBranchIsReported() {
        BranchGate gate = new BranchGate(List.of("master", "F_GAMC-18709_old"));

        List<String> stale = gate.staleEntries(List.of("master", "F_GAMC-19572_notifications"));

        assertThat(stale).containsExactly("F_GAMC-18709_old");
    }

    @Test
    void globEntriesAreExemptFromTheStaleCheck() {
        BranchGate gate = new BranchGate(List.of("release/*"));

        List<String> stale = gate.staleEntries(List.of("master"));

        assertThat(stale).isEmpty();
    }
}
