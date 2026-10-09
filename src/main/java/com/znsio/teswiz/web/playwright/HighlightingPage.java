package com.znsio.teswiz.web.playwright;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * A transparent {@link Page} that makes element highlighting universal for the Playwright-Java
 * engine. It is installed once, at the single point where the {@code Page} is created, so a test
 * Screen keeps writing {@code context.page().locator(selector).click()} on the raw Playwright API
 * and still gets a highlight before the action — no explicit call, no Selenium types.
 *
 * <p>Every {@link Locator} / {@link FrameLocator} the page hands out ({@code locator},
 * {@code frameLocator}, {@code getByRole}/{@code getByText}/…) is wrapped in a highlighting proxy,
 * so the highlight fires from the locator layer — which is exactly where the Screens act
 * ({@code page().locator(sel).click()}). Methods that return a new {@code Page} (e.g. {@code opener})
 * are re-wrapped so popups inherit the behaviour. All other methods forward verbatim, so unknown
 * future Playwright methods keep working and the wrapper does not break on version upgrades.
 */
public final class HighlightingPage {

    private HighlightingPage() {
    }

    public static Page wrap(Page delegate) {
        if (delegate instanceof HighlightingPageMarker) {
            return delegate;
        }
        return (Page) Proxy.newProxyInstance(HighlightingPage.class.getClassLoader(),
                new Class<?>[] { Page.class, HighlightingPageMarker.class }, new HighlightingPageHandler(delegate));
    }

    /** Marker so an already-wrapped page is not wrapped twice. */
    private interface HighlightingPageMarker {
    }

    private static final class HighlightingPageHandler implements InvocationHandler {
        private final Page delegate;

        private HighlightingPageHandler(Page delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object result;
            try {
                result = method.invoke(delegate, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
            if (result instanceof Locator locatorResult) {
                return HighlightingLocator.wrap(locatorResult);
            }
            if (result instanceof FrameLocator frameLocatorResult) {
                return HighlightingFrameLocator.wrap(frameLocatorResult);
            }
            if (result instanceof Page pageResult) {
                return wrap(pageResult);
            }
            return result;
        }
    }
}
