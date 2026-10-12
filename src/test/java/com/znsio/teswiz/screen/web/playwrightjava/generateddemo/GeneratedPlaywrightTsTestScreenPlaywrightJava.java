package com.znsio.teswiz.screen.web.playwrightjava.generateddemo;

import java.util.List;

import com.microsoft.playwright.Locator;
import com.znsio.teswiz.screen.generateddemo.GeneratedPlaywrightTsTestScreen;
import com.znsio.teswiz.web.playwright.screen.PlaywrightJavaScreenContext;

public class GeneratedPlaywrightTsTestScreenPlaywrightJava extends GeneratedPlaywrightTsTestScreen {
    private static final String INPUT = "#demo-input";
    private static final String OUTPUT = "#demo-output";

    private final PlaywrightJavaScreenContext context;

    public GeneratedPlaywrightTsTestScreenPlaywrightJava(PlaywrightJavaScreenContext context) {
        this.context = context;
    }

    @Override
    public GeneratedPlaywrightTsTestScreen open(String url) {
        context.page().navigate(url);
        return this;
    }

    @Override
    public GeneratedPlaywrightTsTestScreen enterValue(String value) {
        Locator input = context.page().locator(INPUT);
        input.clear();
        input.fill(value);
        context.page().locator(OUTPUT).evaluate("(element, nextValue) => element.textContent = nextValue", value);
        return this;
    }

    @Override
    public String readValue() {
        String currentValue = context.page().locator(OUTPUT).textContent();
        return null == currentValue ? "" : currentValue.trim();
    }

    @Override
    public List<String> readValues() {
        String currentValue = readValue();
        return List.of(currentValue, currentValue + "-copy");
    }
}
