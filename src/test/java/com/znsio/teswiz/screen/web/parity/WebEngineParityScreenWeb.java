package com.znsio.teswiz.screen.web.parity;

import java.util.Set;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;

import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.Visual;
import com.znsio.teswiz.screen.parity.WebEngineParityScreen;

public class WebEngineParityScreenWeb extends WebEngineParityScreen {
    private final Driver driver;
    private final Visual visually;

    public WebEngineParityScreenWeb(Driver driver, Visual visually) {
        this.driver = driver;
        this.visually = visually;
    }

    @Override
    public WebEngineParityScreen addCookie(String key, String value) {
        driver.getInnerDriver().manage().addCookie(new Cookie(key, value));
        return this;
    }

    @Override
    public boolean isCookiePresent(String key) {
        Set<Cookie> cookies = driver.getInnerDriver().manage().getCookies();
        return cookies.stream().anyMatch(cookie -> cookie.getName().equals(key));
    }

    @Override
    public WebEngineParityScreen deleteCookie(String key) {
        driver.getInnerDriver().manage().deleteCookieNamed(key);
        return this;
    }

    @Override
    public WebEngineParityScreen setViewport(int width, int height) {
        driver.getInnerDriver().manage().window().setSize(new Dimension(width, height));
        return this;
    }

    @Override
    public int[] getViewportSize() {
        Dimension size = driver.getInnerDriver().manage().window().getSize();
        return new int[] { size.getWidth(), size.getHeight() };
    }

    @Override
    public Object executeAsyncScript(int delayMs, String returnVal) {
        JavascriptExecutor executor = (JavascriptExecutor) driver.getInnerDriver();
        String script = String.format(
                "var callback = arguments[arguments.length - 1];" +
                "setTimeout(function() { callback('%s'); }, %d);", returnVal, delayMs);
        return executor.executeAsyncScript(script);
    }
}
