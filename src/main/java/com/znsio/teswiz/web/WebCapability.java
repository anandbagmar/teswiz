package com.znsio.teswiz.web;

public enum WebCapability {
    FRAMES("switchTo().frame / parentFrame",
            "Use context.page().frameLocator(selector) from PlaywrightJavaScreenContext"),
    WINDOW_RESIZE("manage().window().setSize",
            "Use viewport configuration in browser_config.json or TESWIZ_DRIVER_VIEWPORT_WIDTH/HEIGHT"),
    WINDOW_POSITION("manage().window().setPosition", "Window positioning is managed by browser launch configuration"),
    COOKIE_MANAGEMENT("manage().addCookie / getCookies / deleteCookie",
            "Use context.browserContext().addCookies / cookies / clearCookies"),
    ASYNC_SCRIPT("executeAsyncScript", "Use page.evaluate() with Promise or async functions"),
    MULTIPLE_WINDOWS("switchTo().window / newWindow",
            "Use context.page().context().pages() or context.page().context().newPage()"),
    VIEWPORT_OVERRIDE("viewport override in contextOptions", "Configure viewport in browser_config.json"),
    NETWORK_INTERCEPTION("network routing & request interception",
            "Use context.page().route(...) from PlaywrightJavaScreenContext"),
    SHADOW_DOM("getShadowRoot",
            "Use context.page().locator(...) with Playwright's piercing CSS (shadow DOM is traversed automatically)"),
    ALERTS("switchTo().alert / waitForAlert", "Use context.page().onDialog(...) to handle native dialogs"),
    ELEMENT_LOCATION_BY_SELENIUM_LOCATOR("findElement(By) / findElements(By) and By-based waits",
            "Author Playwright screens natively: pw-java via context.page().locator(...)/getByText(...) "
                    + "from PlaywrightJavaScreenContext; pw-ts via its .screen.ts modules. "
                    + "The Selenium By bridge is Selenium/Appium-only.");

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
