package com.znsio.teswiz.screen.web.playwrightjava.parity;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;

import com.znsio.teswiz.runner.Visual;
import com.znsio.teswiz.screen.parity.WebEngineParityScreen;
import com.znsio.teswiz.web.playwright.PlaywrightJavaWebDriver;
import com.znsio.teswiz.web.playwright.screen.PlaywrightJavaScreenContext;

public class WebEngineParityScreenPlaywrightJava extends WebEngineParityScreen {
    private final PlaywrightJavaWebDriver driver;
    private final Visual visually;

    public WebEngineParityScreenPlaywrightJava(PlaywrightJavaScreenContext context) {
        this.driver = (PlaywrightJavaWebDriver) context.driver().getInnerDriver();
        this.visually = context.visual();
    }

    @Override
    public WebEngineParityScreen addCookie(String key, String value) {
        driver.manage().addCookie(new Cookie(key, value));
        return this;
    }

    @Override
    public boolean isCookiePresent(String key) {
        return driver.manage().getCookies().stream()
                .anyMatch(cookie -> cookie.getName().equals(key));
    }

    @Override
    public WebEngineParityScreen deleteCookie(String key) {
        driver.manage().deleteCookieNamed(key);
        return this;
    }

    @Override
    public WebEngineParityScreen setViewport(int width, int height) {
        driver.manage().window().setSize(new Dimension(width, height));
        return this;
    }

    @Override
    public int[] getViewportSize() {
        Dimension size = driver.manage().window().getSize();
        return new int[] { size.getWidth(), size.getHeight() };
    }

    @Override
    public Object executeAsyncScript(int delayMs, String returnVal) {
        String script = String.format(
                "var callback = arguments[arguments.length - 1];" +
                "setTimeout(function() { callback('%s'); }, %d);", returnVal, delayMs);
        return driver.executeAsyncScript(script);
    }
}
