package com.znsio.teswiz.listener;

import java.util.ArrayList;
import java.util.List;

import com.znsio.teswiz.context.SessionContext;
import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.TEST_CONTEXT;
import com.znsio.teswiz.network.NetworkNote;
import com.znsio.teswiz.tools.ReportPortalLogger;

/**
 * Keeps each network note with the scenario that was running when the connection was made, so the note shows up next
 * to that scenario: as a warning in its ReportPortal log, and in its {@link TestExecutionContext} under
 * {@link TEST_CONTEXT#NETWORK_NOTES} (one line per note), where a consumer's {@code @After} hook can attach it to the
 * Cucumber report. A note recorded outside any scenario (for example by the end-of-run notification) is left to the
 * run-wide summary.
 */
public final class ScenarioNetworkNotes {

    private static final String PREFIX = "Network note: ";

    private ScenarioNetworkNotes() {
    }

    /**
     * @param note a note just recorded on the current thread
     * @return true when a scenario was running on this thread and the note was kept with it
     */
    public static boolean record(NetworkNote note) {
        TestExecutionContext scenario = SessionContext.getTestExecutionContext(Thread.currentThread().getId());
        if (scenario == null) {
            return false;
        }
        List<String> notes = new ArrayList<>(of(scenario));
        notes.add(note.describe());
        scenario.addTestState(TEST_CONTEXT.NETWORK_NOTES, List.copyOf(notes));
        ReportPortalLogger.logWarningMessage(PREFIX + note.describe());
        return true;
    }

    /**
     * @param scenario a scenario's context
     * @return the notes recorded while it ran, one line each, in order (empty when none)
     */
    @SuppressWarnings("unchecked")
    public static List<String> of(TestExecutionContext scenario) {
        Object notes = scenario.getTestState(TEST_CONTEXT.NETWORK_NOTES);
        return notes instanceof List<?> list ? (List<String>) list : List.of();
    }
}
