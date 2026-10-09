package com.znsio.teswiz.web.playwright;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import com.microsoft.playwright.Locator;
import com.znsio.teswiz.runner.Setup;

class HighlightingLocatorTest {

    @AfterEach
    void resetHighlightFlag() {
        Setup.addBooleanValueToConfigs(Setup.HIGHLIGHT_ELEMENTS, true);
    }

    @Test
    void highlightsBeforeDelegatingAnActionMethod() {
        Setup.addBooleanValueToConfigs(Setup.HIGHLIGHT_ELEMENTS, true);
        Locator delegate = Mockito.mock(Locator.class);

        Locator wrapped = HighlightingLocator.wrap(delegate);
        wrapped.click();

        InOrder inOrder = Mockito.inOrder(delegate);
        inOrder.verify(delegate).evaluate(Mockito.anyString()); // the highlight
        inOrder.verify(delegate).click(); // then the real action
    }

    @Test
    void forwardsNonActionMethodsWithoutHighlighting() {
        Setup.addBooleanValueToConfigs(Setup.HIGHLIGHT_ELEMENTS, true);
        Locator delegate = Mockito.mock(Locator.class);
        when(delegate.isVisible()).thenReturn(true);

        boolean visible = HighlightingLocator.wrap(delegate).isVisible();

        assertThat(visible).isTrue();
        verify(delegate).isVisible();
        verify(delegate, Mockito.never()).evaluate(Mockito.anyString());
    }

    @Test
    void reWrapsChainingMethodsSoHighlightSurvivesTheChain() {
        Setup.addBooleanValueToConfigs(Setup.HIGHLIGHT_ELEMENTS, true);
        Locator delegate = Mockito.mock(Locator.class);
        Locator nthDelegate = Mockito.mock(Locator.class);
        when(delegate.nth(0)).thenReturn(nthDelegate);

        Locator chained = HighlightingLocator.wrap(delegate).nth(0);
        chained.click();

        // the chained locator must also highlight before clicking its own delegate
        InOrder inOrder = Mockito.inOrder(nthDelegate);
        inOrder.verify(nthDelegate).evaluate(Mockito.anyString());
        inOrder.verify(nthDelegate).click();
    }
}
