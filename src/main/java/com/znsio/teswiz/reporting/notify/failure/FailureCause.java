package com.znsio.teswiz.reporting.notify.failure;

/**
 * Why a scenario failed: the kind of exception and, when it can be told, the place in the code it came from. Two
 * scenarios with an equal cause failed for the same reason, which is what lets a breakdown say "eight scenarios, one
 * cause" instead of listing eight failures.
 *
 * @param type     the simple exception class name, or {@link FailureClassifier#UNCLASSIFIED}
 * @param location the originating place as {@code Class.method:line}, or an empty string when it cannot be told
 */
public record FailureCause(String type, String location) {

    /**
     * Canonical constructor; a null component becomes the empty string so a cause is always safe to print and compare.
     */
    public FailureCause {
        type = type == null ? "" : type;
        location = location == null ? "" : location;
    }

    /**
     * @return the type and, when known, the place — {@code IndexOutOfBoundsException @ RewardsBL.awardFreeSpins:113}
     *         — or just the type when there is no place
     */
    public String label() {
        return location.isEmpty() ? type : type + " @ " + location;
    }
}
