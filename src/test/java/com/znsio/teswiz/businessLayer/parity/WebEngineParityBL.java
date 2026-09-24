package com.znsio.teswiz.businessLayer.parity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.core.api.SoftAssertions;

import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.Platform;
import com.znsio.teswiz.entities.SAMPLE_TEST_CONTEXT;
import com.znsio.teswiz.runner.Runner;
import com.znsio.teswiz.screen.parity.WebEngineParityScreen;

public class WebEngineParityBL {
    private static final Logger LOGGER = LogManager.getLogger(WebEngineParityBL.class.getName());
    private final TestExecutionContext context;
    private final SoftAssertions softly;
    private final String currentUserPersona;
    private final Platform currentPlatform;

    public WebEngineParityBL(String userPersona, Platform forPlatform) {
        long threadId = Thread.currentThread().getId();
        this.context = Runner.getTestExecutionContext(threadId);
        softly = Runner.getSoftAssertion(threadId);
        this.currentUserPersona = userPersona;
        this.currentPlatform = forPlatform;
        Runner.setCurrentDriverForUser(userPersona, forPlatform, context);
    }

    public WebEngineParityBL() {
        long threadId = Thread.currentThread().getId();
        this.context = Runner.getTestExecutionContext(threadId);
        softly = Runner.getSoftAssertion(threadId);
        this.currentUserPersona = SAMPLE_TEST_CONTEXT.ME;
        this.currentPlatform = Runner.getPlatform();
    }

    public WebEngineParityBL addSessionCookie(String key, String value) {
        LOGGER.info(String.format("Adding cookie '%s' = '%s'", key, value));
        WebEngineParityScreen.get().addCookie(key, value);
        return this;
    }

    public WebEngineParityBL verifyCookiePresent(String key) {
        LOGGER.info(String.format("Verifying cookie '%s' is present", key));
        boolean isPresent = WebEngineParityScreen.get().isCookiePresent(key);
        softly.assertThat(isPresent).as("Cookie '" + key + "' should be present in session").isTrue();
        return this;
    }

    public WebEngineParityBL deleteSessionCookie(String key) {
        LOGGER.info(String.format("Deleting cookie '%s'", key));
        WebEngineParityScreen.get().deleteCookie(key);
        return this;
    }

    public WebEngineParityBL verifyCookieNotPresent(String key) {
        LOGGER.info(String.format("Verifying cookie '%s' is not present", key));
        boolean isPresent = WebEngineParityScreen.get().isCookiePresent(key);
        softly.assertThat(isPresent).as("Cookie '" + key + "' should not be present in session").isFalse();
        return this;
    }

    public WebEngineParityBL setViewportSize(int width, int height) {
        LOGGER.info(String.format("Setting viewport size to %dx%d", width, height));
        WebEngineParityScreen.get().setViewport(width, height);
        return this;
    }

    public WebEngineParityBL verifyViewportSize(int expectedWidth, int expectedHeight) {
        LOGGER.info(String.format("Verifying viewport size is %dx%d", expectedWidth, expectedHeight));
        int[] actualSize = WebEngineParityScreen.get().getViewportSize();
        softly.assertThat(actualSize[0]).as("Viewport width").isEqualTo(expectedWidth);
        softly.assertThat(actualSize[1]).as("Viewport height").isEqualTo(expectedHeight);
        return this;
    }

    public WebEngineParityBL executeAsyncScript(int delayMs, String expectedReturn) {
        LOGGER.info(String.format("Executing async script with %dms delay, expecting return '%s'", delayMs, expectedReturn));
        Object result = WebEngineParityScreen.get().executeAsyncScript(delayMs, expectedReturn);
        context.addTestState("asyncScriptResult", result);
        return this;
    }

    public WebEngineParityBL verifyAsyncScriptResult(String expectedReturn) {
        LOGGER.info(String.format("Verifying async script result matches '%s'", expectedReturn));
        Object result = context.getTestState("asyncScriptResult");
        softly.assertThat(result).as("Async script result").isEqualTo(expectedReturn);
        return this;
    }
}
