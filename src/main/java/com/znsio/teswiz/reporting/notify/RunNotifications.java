package com.znsio.teswiz.reporting.notify;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.znsio.teswiz.exceptions.NotificationException;

/**
 * The end-of-run notification entry point teswiz invokes from its {@code Runner} after the cucumber report is complete
 * and before the JVM exits. It is entirely opt-in and self-contained: when a consuming project configures a
 * {@link RunNotificationComposer} (via the {@code notify.composer.class} setting in {@code notifications.properties}),
 * teswiz loads it, hands it the finished {@link RunResult}, and delivers whatever {@link Notification}s it returns
 * through the {@link Notifier}. When no composer is configured — or the settings resource is absent — teswiz does
 * nothing, so a project that does not use notifications pays no cost.
 * <p>
 * Failure handling is governed by the {@code notify.fail.on.error} setting. By default ({@code false}) every failure
 * here is logged and swallowed, so a notifier problem — a transient Slack/SMTP blip, a bad composer, an undeliverable
 * message — never changes a run's pass/fail result. When {@code notify.fail.on.error=true}, a run that <em>wanted</em>
 * to notify but could not (a delivery failure, a missing destination for an enabled channel, or a broken composer)
 * fails fast with a {@link NotificationException} so the misconfiguration is caught immediately rather than passing
 * silently. A clean (or deliberately-disabled) notification never fails the run either way.
 */
public final class RunNotifications {

    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager
            .getLogger(RunNotifications.class.getName());

    private static final String SETTINGS_RESOURCE = "notifications.properties";
    private static final String COMPOSER_CLASS_KEY = "notify.composer.class";
    private static final String FAIL_ON_ERROR_KEY = "notify.fail.on.error";
    private static final String LOG_DIR_PROPERTY = "LOG_DIR";
    private static final String CI_BRANCH_ENV = "CI_COMMIT_REF_NAME";
    private static final String REPORTS_DIR = "reports";
    private static final String JUNIT_REPORT_FILE = "cucumber-junit-report.xml";
    private static final String NOT_SET = "not-set";

    private RunNotifications() {
    }

    /**
     * Fires the configured run notification, if any. Loads {@code notifications.properties}, resolves the composer
     * named by {@code notify.composer.class}, resolves the completed cucumber JUnit XML from {@code LOG_DIR}, builds
     * the {@link RunResult}, and delivers the composed messages. A no-op (logged at debug) when nothing is configured.
     * Swallows every failure by default; throws {@link NotificationException} only when {@code notify.fail.on.error}
     * is set and the run wanted to notify but could not.
     *
     * @param totalScenarios  the total scenarios executed
     * @param passedScenarios the passed scenarios
     * @param failedScenarios the failed scenarios
     */
    public static void fireIfConfigured(int totalScenarios, int passedScenarios, int failedScenarios) {
        boolean failOnError = false;
        try {
            NotificationSettings settings = loadSettingsOrNull();
            if (settings == null) {
                return;
            }
            failOnError = settings.isTrue(FAIL_ON_ERROR_KEY);
            String composerClass = settings.get(COMPOSER_CLASS_KEY);
            if (composerClass.isEmpty()) {
                LOGGER.debug("No {} configured; skipping run notifications.", COMPOSER_CLASS_KEY);
                return;
            }
            RunNotificationComposer composer = instantiateOrFail(composerClass);
            RunResult result = buildRunResult(totalScenarios, passedScenarios, failedScenarios);
            List<Notification> notifications = composer.compose(result);
            Map<String, NotificationOutcome> outcomes = new Notifier(settings, result.runEnvironment().branch())
                    .sendAll(notifications);
            LOGGER.info("Run notification outcomes: {}", outcomes);
            escalateIfConfigured(outcomes, failOnError);
        } catch (NotificationException e) {
            // A deliberate fail-fast (notify.fail.on.error=true): let it escape so the run fails with a clear reason.
            throw e;
        } catch (Exception e) {
            handleUnexpectedFailure(e, failOnError);
        }
    }

    /**
     * Applies the opt-in fail-fast policy. When {@code failOnError} is {@code true}, any outcome meaning "the run
     * wanted to notify but could not" — a {@link NotificationOutcome#FAILED} delivery or a
     * {@link NotificationOutcome#SKIPPED_NO_DESTINATION} for an enabled channel — throws {@link NotificationException}
     * so the run fails fast. A fully-delivered (or deliberately skipped-as-disabled) set never throws; neither does an
     * empty map. When {@code failOnError} is {@code false} (the default) this is a no-op and problems stay swallowed.
     *
     * @param outcomes    the per-target outcomes from {@link Notifier#sendAll(List)}
     * @param failOnError whether {@code notify.fail.on.error} is set
     */
    static void escalateIfConfigured(Map<String, NotificationOutcome> outcomes, boolean failOnError) {
        if (!failOnError || outcomes == null || outcomes.isEmpty()) {
            return;
        }
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, NotificationOutcome> entry : outcomes.entrySet()) {
            if (isProblem(entry.getValue())) {
                problems.add(entry.getKey() + "=" + entry.getValue());
            }
        }
        if (!problems.isEmpty()) {
            throw new NotificationException("Run notification could not be delivered and notify.fail.on.error is set: "
                    + String.join(", ", problems));
        }
    }

    private static boolean isProblem(NotificationOutcome outcome) {
        return outcome == NotificationOutcome.FAILED || outcome == NotificationOutcome.SKIPPED_NO_DESTINATION;
    }

    private static void handleUnexpectedFailure(Exception e, boolean failOnError) {
        if (failOnError) {
            throw new NotificationException("Run notification failed and notify.fail.on.error is set: "
                    + e.getMessage(), e);
        }
        LOGGER.warn("Run notification failed; swallowed — the run result is NOT affected: {}", e.getMessage());
    }

    private static NotificationSettings loadSettingsOrNull() {
        try {
            return NotificationSettings.load(SETTINGS_RESOURCE);
        } catch (RuntimeException noSettings) {
            LOGGER.debug("No {} on the classpath; skipping run notifications.", SETTINGS_RESOURCE);
            return null;
        }
    }

    private static RunNotificationComposer instantiateOrFail(String composerClass) {
        try {
            Class<?> type = Class.forName(composerClass);
            Object instance = type.getDeclaredConstructor().newInstance();
            if (instance instanceof RunNotificationComposer composer) {
                return composer;
            }
            throw new NotificationException(
                    composerClass + " configured as " + COMPOSER_CLASS_KEY + " is not a RunNotificationComposer.");
        } catch (ReflectiveOperationException e) {
            throw new NotificationException("Could not instantiate run notification composer '" + composerClass
                    + "' (it must have a public no-arg constructor): " + e.getMessage(), e);
        }
    }

    private static RunResult buildRunResult(int totalScenarios, int passedScenarios, int failedScenarios) {
        Map<String, String> environment = System.getenv();
        String logDir = System.getProperty(LOG_DIR_PROPERTY, "");
        Path logDirPath = logDir.isEmpty() ? null : Path.of(logDir);
        Path junitReportXml = resolveJunitReport(logDirPath);
        RunEnvironment runEnvironment = RunEnvironment.resolve(environment, resolveBranch(environment),
                RunNotifications::hostName, RunNotifications::userName);
        return new RunResult(junitReportXml, logDirPath, totalScenarios, passedScenarios, failedScenarios, environment,
                runEnvironment);
    }

    private static Path resolveJunitReport(Path logDirPath) {
        if (logDirPath == null) {
            return null;
        }
        Path report = logDirPath.resolve(REPORTS_DIR).resolve(JUNIT_REPORT_FILE);
        return JUnitResults.isReadable(report) ? report : null;
    }

    private static String resolveBranch(Map<String, String> environment) {
        String ciRef = environment.get(CI_BRANCH_ENV);
        if (ciRef != null && !ciRef.trim().isEmpty()) {
            return ciRef.trim();
        }
        return branchFromSetup();
    }

    /**
     * Falls back to the branch teswiz already resolved during {@code Setup} (the {@code BRANCH_NAME} config, which is
     * itself the {@code BRANCH_NAME} env/property else {@code git rev-parse --abbrev-ref HEAD}). This keeps branch
     * resolution in one place — teswiz's {@code Setup} — instead of re-running git here. Returns an empty string when
     * it is unset/unresolved so the branch gate behaves as "no branch". {@code Setup} is referenced fully-qualified on
     * purpose: an auto-import-organizer keeps stripping an added import from this file.
     */
    private static String branchFromSetup() {
        try {
            String branch = com.znsio.teswiz.runner.Setup.getFromConfigs(com.znsio.teswiz.runner.Setup.BRANCH_NAME);
            if (branch == null || branch.isBlank() || NOT_SET.equalsIgnoreCase(branch.trim())) {
                return "";
            }
            return branch.trim();
        } catch (RuntimeException e) {
            LOGGER.debug("Could not read the branch from Setup; branch left empty: {}", e.getMessage());
            return "";
        }
    }

    private static String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "";
        }
    }

    private static String userName() {
        return System.getProperty("user.name", "");
    }
}
