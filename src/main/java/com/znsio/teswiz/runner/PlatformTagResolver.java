package com.znsio.teswiz.runner;

import java.util.List;

import com.znsio.teswiz.entities.Platform;

/**
 * Resolves the execution {@link Platform} and ReportPortal launch-name suffix implied by a
 * multi-user Cucumber tag (e.g. {@code multiuser-android-web}).
 *
 * <p>Replaces the former {@code if/else} chain in {@link Setup#getPlatformTagsAndLaunchName()} with
 * an ordered table so the mapping lives in one place and is unit-testable in isolation. Order is
 * significant: more specific tags (e.g. {@code multiuser-android-web}) must be checked before less
 * specific ones (e.g. {@code multiuser-android}) because matching is substring-based, mirroring the
 * original chain exactly.
 */
public final class PlatformTagResolver {

    /**
     * Outcome of resolving a multi-user tag: the inferred {@link Platform} and the human-readable
     * launch-name suffix appended for ReportPortal.
     */
    public record Resolution(Platform platform, String launchNameSuffix) {
    }

    private record Rule(String tagToken, Platform platform, String launchNameSuffix) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule("multiuser-android-web", Platform.android, " - Real User Simulation on Android & Web"),
            new Rule("multiuser-android", Platform.android, " - Real User Simulation on multiple Androids"),
            new Rule("multiuser-web", Platform.web, " - Real User Simulation on Web"),
            new Rule("multiuser-electron", Platform.electron, " - Real User Simulation on Electron"),
            new Rule("multiuser-windows-web", Platform.windows, " - Real User Simulation on Windows & Web"),
            new Rule("multiuser-windows-android", Platform.windows, " - Real User Simulation on Windows & Android"),
            new Rule("multiuser-iOS", Platform.iOS, " - Real User Simulation on IOS"));

    private PlatformTagResolver() {
    }

    /**
     * Returns the {@link Resolution} for the first multi-user rule whose token is contained in
     * {@code providedTags}, or {@code null} when no multi-user tag matches (the caller then keeps
     * the current single-user platform).
     */
    public static Resolution resolve(String providedTags) {
        if (null == providedTags) {
            return null;
        }
        for (Rule rule : RULES) {
            if (providedTags.contains(rule.tagToken())) {
                return new Resolution(rule.platform(), rule.launchNameSuffix());
            }
        }
        return null;
    }
}
