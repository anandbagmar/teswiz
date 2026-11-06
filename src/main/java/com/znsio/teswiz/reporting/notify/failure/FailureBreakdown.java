package com.znsio.teswiz.reporting.notify.failure;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.znsio.teswiz.reporting.notify.JUnitResults.FeatureScenarios;
import com.znsio.teswiz.reporting.notify.JUnitResults.Scenario;

/**
 * The "why did things fail?" tally: failed and errored scenarios counted by {@link FailureCause}, biggest cause first,
 * ties broken by type then place so the output is stable. The total always equals the number of failed and errored
 * scenarios — a failure that cannot be classified is counted under {@link FailureClassifier#UNCLASSIFIED}, never
 * dropped — so the breakdown can never disagree with the headline failure count.
 * <p>
 * Build one per run with {@link #of(List)} (or {@code JUnitResults.failureBreakdown()}); merge breakdowns from several
 * runs with {@link #plus(FailureBreakdown)}. Render it with {@link FailureBreakdownRenderer}.
 */
public final class FailureBreakdown {

    private static final FailureBreakdown EMPTY = new FailureBreakdown(Map.of());

    private static final Comparator<FailureCause> BY_TYPE_THEN_LOCATION_THEN_STEP = Comparator
            .comparing(FailureCause::type).thenComparing(FailureCause::location).thenComparing(FailureCause::step);

    // Insertion-ordered and unmodifiable: ordered biggest-first when built, and callers render it in that order.
    private final Map<FailureCause, Integer> causes;

    private FailureBreakdown(Map<FailureCause, Integer> causes) {
        this.causes = causes;
    }

    /**
     * @return the breakdown with no failures — the identity for {@link #plus(FailureBreakdown)}
     */
    public static FailureBreakdown empty() {
        return EMPTY;
    }

    /**
     * Builds the breakdown from the scenarios of one run.
     *
     * @param scenariosByFeature every scenario grouped by feature, as parsed from the run's JUnit XML
     * @return the tally of the failed and errored scenarios by cause
     */
    public static FailureBreakdown of(List<FeatureScenarios> scenariosByFeature) {
        Map<FailureCause, Integer> tally = new HashMap<>();
        for (FeatureScenarios feature : scenariosByFeature) {
            for (Scenario scenario : feature.scenarios()) {
                if (scenario.failed()) {
                    tally.merge(FailureClassifier.classify(scenario), 1, Integer::sum);
                }
            }
        }
        return fromTally(tally);
    }

    /**
     * Merges this breakdown with another, adding the counts of shared causes — the aggregate across several runs.
     *
     * @param other the other breakdown
     * @return a new breakdown with the two tallies added
     */
    public FailureBreakdown plus(FailureBreakdown other) {
        Map<FailureCause, Integer> tally = new HashMap<>(causes);
        other.causes.forEach((cause, count) -> tally.merge(cause, count, Integer::sum));
        return fromTally(tally);
    }

    /**
     * @return the count for each cause, biggest first then by type then place, as an unmodifiable ordered map
     */
    public Map<FailureCause, Integer> causes() {
        return causes;
    }

    /**
     * @return the same failures counted by exception type alone (places merged), biggest first then by type
     */
    public Map<String, Integer> byType() {
        Map<String, Integer> tally = new HashMap<>();
        causes.forEach((cause, count) -> tally.merge(cause.type(), count, Integer::sum));
        return biggestFirst(tally, Comparator.naturalOrder());
    }

    /**
     * @return the total number of failed and errored scenarios across all causes
     */
    public int total() {
        return causes.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * @return {@code true} when there were no failures to break down
     */
    public boolean isEmpty() {
        return causes.isEmpty();
    }

    private static FailureBreakdown fromTally(Map<FailureCause, Integer> tally) {
        return tally.isEmpty() ? EMPTY : new FailureBreakdown(biggestFirst(tally, BY_TYPE_THEN_LOCATION_THEN_STEP));
    }

    // Re-orders a tally by descending count, ties by the given key order, into an insertion-ordered map wrapped
    // unmodifiable (Map.copyOf would not preserve that order).
    private static <K> Map<K, Integer> biggestFirst(Map<K, Integer> tally, Comparator<K> tieBreak) {
        Comparator<Map.Entry<K, Integer>> order = Map.Entry.<K, Integer>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry::getKey, tieBreak);
        Map<K, Integer> ordered = new LinkedHashMap<>();
        tally.entrySet().stream().sorted(order).forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
        return Collections.unmodifiableMap(ordered);
    }
}
