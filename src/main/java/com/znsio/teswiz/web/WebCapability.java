package com.znsio.teswiz.web;

public enum WebCapability {
    FRAMES("switchTo().frame / parentFrame", "Use context.page().frameLocator(selector) from PlaywrightJavaScreenContext"),
    WINDOW_RESIZE("manage().window().setSize", "Use viewport configuration in browser_config.json or TESWIZ_DRIVER_VIEWPORT_WIDTH/HEIGHT"),
    WINDOW_POSITION("manage().window().setPosition", "Window positioning is managed by browser launch configuration"),
    COOKIE_MANAGEMENT("manage().addCookie / getCookies / deleteCookie", "Use context.browserContext().addCookies / cookies / clearCookies"),
    ASYNC_SCRIPT("executeAsyncScript", "Use page.evaluate() with Promise or async functions"),
    MULTIPLE_WINDOWS("switchTo().window / newWindow", "Use context.page().context().pages() or context.page().context().newPage()"),
    VIEWPORT_OVERRIDE("viewport override in contextOptions", "Configure viewport in browser_config.json"),
    NETWORK_INTERCEPTION("network routing & request interception", "Use context.page().route(...) from PlaywrightJavaScreenContext");

    private final String description;
    private final String nativeAlternative;

    WebCapability(String description, String nativeAlternative) {
        this.description = description;
        this.nativeAlternative = nativeAlternative;
    }

    public String getDescription() {
        return description;
    }

    public String getNativeAlternative() {
        return nativeAlternative;
    }
}
