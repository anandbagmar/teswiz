package com.znsio.teswiz.web.playwright;

import java.util.LinkedHashMap;
import java.util.Map;

import org.openqa.selenium.By;

/**
 * Translates a Selenium {@link By} (the uniform locator currency across teswiz web engines) into the strategy/value
 * pair the Playwright engine understands.
 *
 * <p>
 * Playwright-native locators created via {@link PlaywrightBy} carry their strategy and value as structured fields, so
 * they are translated directly. Standard Selenium {@code By} subtypes expose their value only through
 * {@code toString()} (Selenium provides no public getter), so those are mapped by their documented
 * {@code "By.<strategy>: "} prefix via a single lookup table rather than a chain of conditionals.
 */
record PlaywrightLocator(String strategy, String value) {

    private static final Map<String, String> SELENIUM_PREFIX_TO_STRATEGY = buildPrefixToStrategyMap();

    static PlaywrightLocator from(By by) {
        if (PlaywrightBy.isCustom(by)) {
            return new PlaywrightLocator(PlaywrightBy.getStrategy(by), PlaywrightBy.getValue(by));
        }
        String locator = by.toString();
        for (Map.Entry<String, String> entry : SELENIUM_PREFIX_TO_STRATEGY.entrySet()) {
            String prefix = entry.getKey();
            if (locator.startsWith(prefix)) {
                return new PlaywrightLocator(entry.getValue(), locator.substring(prefix.length()));
            }
        }
        throw new UnsupportedOperationException("Unsupported Playwright locator: " + locator);
    }

    private static Map<String, String> buildPrefixToStrategyMap() {
        Map<String, String> prefixToStrategy = new LinkedHashMap<>();
        prefixToStrategy.put("By.id: ", "id");
        prefixToStrategy.put("By.cssSelector: ", "css");
        prefixToStrategy.put("By.xpath: ", "xpath");
        prefixToStrategy.put("By.className: ", "className");
        prefixToStrategy.put("By.name: ", "name");
        prefixToStrategy.put("By.tagName: ", "tagName");
        prefixToStrategy.put("By.linkText: ", "linkText");
        prefixToStrategy.put("By.partialLinkText: ", "partialLinkText");
        return prefixToStrategy;
    }
}
