package com.znsio.teswiz.config.browser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.TEST_CONTEXT;
import com.znsio.teswiz.exceptions.InvalidTestDataException;
import com.znsio.teswiz.runner.Runner;

public final class PlaywrightBrowserConfigResolver {
    private static final String START_MAXIMIZED = "--start-maximized";
    private static final String HEADLESS = "headless";
    private static final String VIEWPORT = "viewport";
    private static final String IGNORE_HTTPS_ERRORS = "ignoreHTTPSErrors";
    private static final String CHANNEL = "channel";
    private static final String EXECUTABLE_PATH = "executablePath";

    public PlaywrightBrowserConfig resolve(String browserName, TestExecutionContext context) {
        JSONObject browserConfigForBrowserType = loadBrowserConfigFor(browserName, context);
        JSONObject playwrightLaunchOptions = playwrightSection(browserConfigForBrowserType, "launchOptions");
        JSONObject playwrightContextOptions = playwrightSection(browserConfigForBrowserType, "contextOptions");
        JSONObject legacyHeadlessOptions = browserConfigForBrowserType.optJSONObject("headlessOptions");

        boolean headless = resolveHeadless(legacyHeadlessOptions, playwrightLaunchOptions);
        Set<String> launchArgs = resolveLaunchArgs(browserConfigForBrowserType, legacyHeadlessOptions,
                playwrightLaunchOptions, headless);
        boolean isMaximized = isMaximized(browserConfigForBrowserType, launchArgs);
        if (isMaximized) {
            launchArgs.add(START_MAXIMIZED);
        }

        return new PlaywrightBrowserConfig(browserName, headless, new ArrayList<>(launchArgs),
                resolveChannel(playwrightLaunchOptions),
                resolveExecutablePath(browserConfigForBrowserType, playwrightLaunchOptions),
                resolveContextOptions(browserConfigForBrowserType, playwrightContextOptions, isMaximized),
                resolveLaunchOptions(browserConfigForBrowserType, playwrightLaunchOptions));
    }

    private JSONObject loadBrowserConfigFor(String browserName, TestExecutionContext context) {
        JSONObject browserConfig = BrowserConfigLoader.load(context);
        maybeRegisterMigrationArtifact(context, browserConfig);
        String browserKey = browserName.toLowerCase();
        if (!browserConfig.has(browserKey)) {
            throw new InvalidTestDataException(
                    String.format("Browser: '%s' is NOT supported for Playwright configuration", browserName));
        }
        return browserConfig.getJSONObject(browserKey);
    }

    // No "playwright" section -> empty options; a section without this key -> null (key not configured).
    private JSONObject playwrightSection(JSONObject browserConfigForBrowserType, String key) {
        JSONObject playwrightConfig = browserConfigForBrowserType.optJSONObject("playwright");
        return null == playwrightConfig ? new JSONObject() : playwrightConfig.optJSONObject(key);
    }

    private boolean resolveHeadless(JSONObject legacyHeadlessOptions, JSONObject playwrightLaunchOptions) {
        boolean configuredHeadless = (null != playwrightLaunchOptions && playwrightLaunchOptions.has(HEADLESS))
                ? playwrightLaunchOptions.getBoolean(HEADLESS)
                : null != legacyHeadlessOptions && legacyHeadlessOptions.optBoolean(HEADLESS, false);
        return configuredHeadless || Runner.isRunningInHeadlessMode();
    }

    private Set<String> resolveLaunchArgs(JSONObject browserConfigForBrowserType, JSONObject legacyHeadlessOptions,
            JSONObject playwrightLaunchOptions, boolean headless) {
        Set<String> launchArgs = new LinkedHashSet<>();
        addNormalizedArguments(browserConfigForBrowserType.optJSONArray("arguments"), launchArgs);
        if (headless && null != legacyHeadlessOptions) {
            addNormalizedArguments(legacyHeadlessOptions.optJSONArray("include"), launchArgs);
        }
        if (null != playwrightLaunchOptions) {
            addNormalizedArguments(playwrightLaunchOptions.optJSONArray("args"), launchArgs);
        }
        return launchArgs;
    }

    private boolean isMaximized(JSONObject browserConfigForBrowserType, Set<String> launchArgs) {
        return browserConfigForBrowserType.optBoolean("maximize", false)
                || launchArgs.stream().anyMatch(arg -> arg.contains("start-maximized"));
    }

    private Map<String, Object> resolveContextOptions(JSONObject browserConfigForBrowserType,
            JSONObject playwrightContextOptions, boolean isMaximized) {
        Map<String, Object> contextOptions = new LinkedHashMap<>();
        if (null != playwrightContextOptions) {
            contextOptions.putAll(playwrightContextOptions.toMap());
        }
        if (!contextOptions.containsKey(VIEWPORT)) {
            contextOptions.put(VIEWPORT, defaultViewport(isMaximized));
        }
        contextOptions.put(IGNORE_HTTPS_ERRORS, resolveIgnoreHttpsErrors(browserConfigForBrowserType,
                playwrightContextOptions));
        return contextOptions;
    }

    // A maximized window must not get a fixed viewport, or it would override --start-maximized.
    private Map<String, Object> defaultViewport(boolean isMaximized) {
        if (isMaximized) {
            return null;
        }
        Map<String, Object> viewport = new LinkedHashMap<>();
        viewport.put("width", WindowSizingResolver.defaultWidth());
        viewport.put("height", WindowSizingResolver.defaultHeight());
        return viewport;
    }

    private boolean resolveIgnoreHttpsErrors(JSONObject browserConfigForBrowserType,
            JSONObject playwrightContextOptions) {
        if (null != playwrightContextOptions && playwrightContextOptions.has(IGNORE_HTTPS_ERRORS)) {
            return playwrightContextOptions.getBoolean(IGNORE_HTTPS_ERRORS);
        }
        return browserConfigForBrowserType.optBoolean("acceptInsecureCerts", false);
    }

    private Map<String, Object> resolveLaunchOptions(JSONObject browserConfigForBrowserType,
            JSONObject playwrightLaunchOptions) {
        Map<String, Object> launchOptions = new LinkedHashMap<>();
        if (null != playwrightLaunchOptions) {
            launchOptions.putAll(withoutResolvedLaunchKeys(playwrightLaunchOptions.toMap()));
        }
        String proxyUrl = Runner.getProxyURL();
        if (null != proxyUrl && !proxyUrl.isBlank()) {
            launchOptions.put("proxy", buildProxy(browserConfigForBrowserType, proxyUrl));
        }
        return launchOptions;
    }

    // These launch options are resolved into their own PlaywrightBrowserConfig fields.
    private Map<String, Object> withoutResolvedLaunchKeys(Map<String, Object> configuredLaunchOptions) {
        configuredLaunchOptions.remove(HEADLESS);
        configuredLaunchOptions.remove("args");
        configuredLaunchOptions.remove(CHANNEL);
        configuredLaunchOptions.remove(EXECUTABLE_PATH);
        return configuredLaunchOptions;
    }

    private Map<String, Object> buildProxy(JSONObject browserConfigForBrowserType, String proxyUrl) {
        Map<String, Object> proxy = new LinkedHashMap<>();
        proxy.put("server", proxyUrl);
        if (browserConfigForBrowserType.has("noProxy")) {
            proxy.put("bypass", browserConfigForBrowserType.getString("noProxy"));
        }
        return proxy;
    }

    private String resolveChannel(JSONObject playwrightLaunchOptions) {
        return null == playwrightLaunchOptions ? null : playwrightLaunchOptions.optString(CHANNEL, null);
    }

    private void maybeRegisterMigrationArtifact(TestExecutionContext context, JSONObject browserConfig) {
        String sourceConfigPath = context.getTestStateAsString(TEST_CONTEXT.UPDATED_BROWSER_CONFIG_FILE_FOR_THIS_TEST);
        if (null == sourceConfigPath || sourceConfigPath.isBlank()) {
            sourceConfigPath = Runner.getBrowserConfigFile();
        }
        PlaywrightBrowserConfigMigrationReporter.registerMigrationArtifact(context, sourceConfigPath, browserConfig,
                new PlaywrightBrowserConfigMigrator());
    }

    private void addNormalizedArguments(JSONArray arguments, Set<String> launchArgs) {
        if (null == arguments) {
            return;
        }

        arguments.forEach(argument -> launchArgs.add(normalizeArgument(argument.toString())));
    }

    private String resolveExecutablePath(JSONObject browserConfigForBrowserType, JSONObject playwrightLaunchOptions) {
        if (null != playwrightLaunchOptions) {
            String configuredExecutablePath = playwrightLaunchOptions.optString("executablePath", null);
            if (null != configuredExecutablePath && !configuredExecutablePath.isBlank()) {
                return configuredExecutablePath;
            }
        }
        String legacyBinary = browserConfigForBrowserType.optString("binary", "");
        if (legacyBinary.isBlank()) {
            return null;
        }
        if (browserConfigForBrowserType.optBoolean("electronAppLoadingPage", false)) {
            return null;
        }
        return legacyBinary;
    }

    private String normalizeArgument(String argument) {
        if (argument.startsWith("-")) {
            return argument;
        }
        return "--" + argument;
    }
}
