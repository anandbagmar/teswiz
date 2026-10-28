package com.znsio.teswiz.reporting.notify;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides whether notifications are allowed for the current branch, and reports allowlist entries that have gone stale.
 * <p>
 * The allowlist holds exact branch names and glob patterns (a trailing {@code *}, e.g. {@code release/*}). A branch is
 * allowed when it exactly equals a literal entry or matches a glob pattern. The stale check flags only literal entries
 * that no longer exist among the remote branches — glob patterns are patterns, not branches, so they are exempt. A
 * stale entry is always a warning, never a failure.
 */
public final class BranchGate {

    private static final String GLOB_SUFFIX = "*";

    private final List<String> allowlist;

    /**
     * @param allowlist the configured branch patterns (exact names and/or globs)
     */
    public BranchGate(List<String> allowlist) {
        this.allowlist = List.copyOf(allowlist);
    }

    /**
     * Whether the given branch is permitted by the allowlist.
     *
     * @param branch the current branch name
     * @return {@code true} when the branch matches a literal or glob entry
     */
    public boolean allows(String branch) {
        return allowlist.stream().anyMatch(entry -> matches(entry, branch));
    }

    /**
     * The literal allowlist entries that have no matching remote branch. Glob patterns are excluded (they are patterns,
     * not literal branches).
     *
     * @param remoteBranches the branch names that currently exist on the remote
     * @return the stale literal entries (possibly empty)
     */
    public List<String> staleEntries(List<String> remoteBranches) {
        List<String> stale = new ArrayList<>();
        for (String entry : allowlist) {
            if (!isGlob(entry) && !remoteBranches.contains(entry)) {
                stale.add(entry);
            }
        }
        return stale;
    }

    private static boolean matches(String entry, String branch) {
        if (isGlob(entry)) {
            String prefix = entry.substring(0, entry.length() - GLOB_SUFFIX.length());
            return branch.startsWith(prefix);
        }
        return entry.equals(branch);
    }

    private static boolean isGlob(String entry) {
        return entry.endsWith(GLOB_SUFFIX);
    }
}
