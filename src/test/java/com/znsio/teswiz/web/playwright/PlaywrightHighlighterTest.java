package com.znsio.teswiz.web.playwright;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.microsoft.playwright.Locator;
import com.znsio.teswiz.runner.Setup;

class PlaywrightHighlighterTest {

    @AfterEach
    void resetHighlightFlag() {
        Setup.addBooleanValueToConfigs(Setup.HIGHLIGHT_ELEMENTS, true);
    }

    @Test
    void highlightsTheLocatorWhenHighlightingIsEnabled() {
        Setup.addBooleanValueToConfigs(Setup.HIGHLIGHT_ELEMENTS, true);
        Locator locator = Mockito.mock(Locator.class);

        PlaywrightHighlighter.highlight(locator);

        verify(locator).evaluate(Mockito.anyString());
    }

    @Test
    void doesNothingWhenHighlightingIsDisabled() {
        Setup.addBooleanValueToConfigs(Setup.HIGHLIGHT_ELEMENTS, false);
        Locator locator = Mockito.mock(Locator.class);

        PlaywrightHighlighter.highlight(locator);

        verify(locator, never()).evaluate(Mockito.anyString());
    }

    @Test
    void swallowsEvaluationErrorsSoAnActionIsNeverBlockedByHighlighting() {
        Setup.addBooleanValueToConfigs(Setup.HIGHLIGHT_ELEMENTS, true);
        Locator locator = Mockito.mock(Locator.class);
        when(locator.evaluate(Mockito.anyString())).thenThrow(new RuntimeException("detached element"));

        PlaywrightHighlighter.highlight(locator);

        verify(locator).evaluate(Mockito.anyString());
    }
}
