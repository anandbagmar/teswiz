package com.znsio.teswiz.web;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class WebEngineCapabilities {
    private static final Set<WebCapability> SELENIUM_CAPABILITIES = EnumSet.allOf(WebCapability.class);
    private static final Set<WebCapability> PLAYWRIGHT_TS_CAPABILITIES = EnumSet.allOf(WebCapability.class);
    private static final Set<WebCapability> PLAYWRIGHT_JAVA_CAPABILITIES = EnumSet.of(
            WebCapability.VIEWPORT_OVERRIDE,
            WebCapability.NETWORK_INTERCEPTION,
            WebCapability.FRAMES,
            WebCapability.COOKIE_MANAGEMENT,
            WebCapability.WINDOW_RESIZE,
            WebCapability.ASYNC_SCRIPT
    );

    private static final Map<WebEngine, Set<WebCapability>> CAPABILITY_MAP = Map.of(
            WebEngine.SELENIUM, SELENIUM_CAPABILITIES,
            WebEngine.PLAYWRIGHT_JAVA, PLAYWRIGHT_JAVA_CAPABILITIES,
            WebEngine.PLAYWRIGHT_TS, PLAYWRIGHT_TS_CAPABILITIES
    );

    private WebEngineCapabilities() {
    }

    public static boolean supports(WebEngine engine, WebCapability capability) {
        Set<WebCapability> supported = CAPABILITY_MAP.get(engine);
        return null != supported && supported.contains(capability);
    }

    public static void enableCapability(WebEngine engine, WebCapability capability) {
        Set<WebCapability> supported = CAPABILITY_MAP.get(engine);
        if (null != supported) {
            supported.add(capability);
        }
    }

    public static String formatDiagnosticMessage(WebEngine engine, WebCapability capability, String operationName) {
        return String.format("""
                
                ================================================================================
                [TESWIZ CAPABILITY ERROR] Unsupported Web Capability Requested!
                ================================================================================
                Capability : %s (%s)
                Web Engine : WEB_ENGINE=%s

                REASON:
                  The active engine '%s' does not support traditional Selenium 
                  facade operation '%s'.

                HOW TO RESOLVE:
                  Option 1 (Recommended): Use native Playwright alternative:
                            %s
                  Option 2: Switch to an engine with full capability support:
                            Set WEB_ENGINE=playwright-ts or WEB_ENGINE=selenium in properties.

                DOCUMENTATION:
                  https://github.com/znsio/teswiz/blob/main/docs/web-engine-capabilities.md
                ================================================================================
                """, capability.name(), capability.getDescription(), engine.getConfigValue(),
                engine.getConfigValue(), operationName, capability.getNativeAlternative());
    }
}
