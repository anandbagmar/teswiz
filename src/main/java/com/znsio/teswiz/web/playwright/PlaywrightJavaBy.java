package com.znsio.teswiz.web.playwright;

import org.openqa.selenium.By;

final class PlaywrightJavaBy {
    private PlaywrightJavaBy() {
    }

    static String toSelector(By by) {
        if (PlaywrightBy.isCustom(by)) {
            String strategy = PlaywrightBy.getStrategy(by);
            String val = PlaywrightBy.getValue(by);
            switch (strategy) {
                case "text":
                    return "text=" + val;
                case "role":
                    String[] parts = val.split("\\|", 2);
                    String roleName = parts[0];
                    String name = parts.length > 1 ? parts[1] : "";
                    return name.isEmpty() ? "role=" + roleName : "role=" + roleName + "[name=\"" + escapeQuotes(name) + "\"]";
                case "testId":
                    return "data-testid=" + val + ", [data-test-id=\"" + escapeQuotes(val) + "\"], [data-testid=\"" + escapeQuotes(val) + "\"]";
                case "placeholder":
                    return "[placeholder=\"" + escapeQuotes(val) + "\"]";
                case "label":
                    return "label=" + val;
                case "title":
                    return "[title=\"" + escapeQuotes(val) + "\"]";
                case "altText":
                    return "[alt=\"" + escapeQuotes(val) + "\"]";
                default:
                    return val;
            }
        }

        String value = by.toString();
        if (value.startsWith("By.id: ")) {
            return "#" + cssEscape(value.substring("By.id: ".length()));
        }
        if (value.startsWith("By.name: ")) {
            return "[name=\"" + escapeQuotes(value.substring("By.name: ".length())) + "\"]";
        }
        if (value.startsWith("By.className: ")) {
            return "." + cssEscape(value.substring("By.className: ".length()));
        }
        if (value.startsWith("By.cssSelector: ")) {
            return value.substring("By.cssSelector: ".length());
        }
        if (value.startsWith("By.tagName: ")) {
            return value.substring("By.tagName: ".length());
        }
        if (value.startsWith("By.linkText: ")) {
            return "a:has-text(\"" + escapeQuotes(value.substring("By.linkText: ".length())) + "\")";
        }
        if (value.startsWith("By.xpath: ")) {
            return "xpath=" + value.substring("By.xpath: ".length());
        }
        throw new UnsupportedOperationException("Unsupported locator for Playwright Java: " + value);
    }

    private static String cssEscape(String value) {
        return value.replace(".", "\\.");
    }

    private static String escapeQuotes(String value) {
        return value.replace("\"", "\\\"");
    }
}
