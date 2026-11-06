package com.znsio.teswiz.reporting.notify.failure;

/**
 * Why a scenario failed: the kind of exception, the place in the code it came from (when it can be told) and the
 * Gherkin step that was running when it failed (when it can be told). Two scenarios with an equal cause failed for the
 * same reason, which is what lets a breakdown say "eight scenarios, one cause" instead of listing eight failures.
 * <p>
 * The step is a third grouping dimension on top of type and location: one code location can be reached by several steps
 * (a shared balance assertion reached by "reduced", "increased" and "restored"), so two failures at the same
 * {@code (type, location)} but on different steps are different business causes and get their own rows. A blank step
 * degrades the cause to the old {@code (type, location)} behaviour, so a failure whose step cannot be told still groups
 * exactly as before.
 *
 * @param type     the simple exception class name, or {@link FailureClassifier#UNCLASSIFIED}
 * @param location the originating place as {@code Class.method:line}, or an empty string when it cannot be told
 * @param step     the Gherkin step running when it failed ({@code Then the bet is accepted}), or an empty string when
 *                 it cannot be told
 */
public record FailureCause(String type, String location, String step) {

    /**
     * Canonical constructor; a null component becomes the empty string so a cause is always safe to print and compare.
     */
    public FailureCause {
        type = type == null ? "" : type;
        location = location == null ? "" : location;
        step = step == null ? "" : step;
    }

    /**
     * Convenience constructor for a cause with no known step; the step is the empty string. Keeps callers that only
     * know the type and place (and the many existing tests) working unchanged.
     *
     * @param type     the simple exception class name
     * @param location the originating place, or an empty string
     */
    public FailureCause(String type, String location) {
        this(type, location, "");
    }

    /**
     * @return the type, the place when known and the step when known —
     *         {@code IndexOutOfBoundsException @ RewardsBL.awardFreeSpins:113 — Then the bet is accepted} — dropping
     *         whichever parts are absent
     */
    public String label() {
        StringBuilder label = new StringBuilder(type);
        if (!location.isEmpty()) {
            label.append(" @ ").append(location);
        }
        if (!step.isEmpty()) {
            label.append(" \u2014 ").append(step);
        }
        return label.toString();
    }
}
