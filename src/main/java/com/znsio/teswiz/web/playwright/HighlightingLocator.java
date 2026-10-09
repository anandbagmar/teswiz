package com.znsio.teswiz.web.playwright;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;

/**
 * A transparent {@link Locator} that highlights the element before every user-facing action, so a
 * test driving the raw Playwright {@code page().locator(...)} API gets the same visual highlight the
 * {@code WebElement} adapter gives — with no change to the calling Screen.
 *
 * <p>Implemented as a JDK dynamic proxy rather than a hand-written 80-method wrapper: action methods
 * (click, fill, press, ...) run {@link PlaywrightHighlighter#highlight(Locator)} on the delegate
 * first; methods that return a {@link Locator} or {@link FrameLocator} (nth/first/filter/getBy...)
 * are re-wrapped so highlighting survives locator chaining; every other method forwards verbatim.
 * Unknown future Playwright methods forward by default, so the wrapper does not break on upgrades.
 */
final class HighlightingLocator {

    private static final Set<String> ACTION_METHODS = Set.of("click", "dblclick", "fill", "type", "press",
            "pressSequentially", "check", "uncheck", "setChecked", "selectOption", "selectText", "setInputFiles",
            "hover", "tap", "focus", "clear", "dragTo");

    private HighlightingLocator() {
    }

    static Locator wrap(Locator delegate) {
        if (delegate instanceof HighlightingHandlerMarker) {
            return delegate;
        }
        return (Locator) Proxy.newProxyInstance(HighlightingLocator.class.getClassLoader(),
                new Class<?>[] { Locator.class, HighlightingHandlerMarker.class }, new HighlightingHandler(delegate));
    }

    /** Marker so an already-wrapped locator is not wrapped twice. */
    private interface HighlightingHandlerMarker {
    }

    private static final class HighlightingHandler implements InvocationHandler {
        private final Locator delegate;

        private HighlightingHandler(Locator delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (ACTION_METHODS.contains(method.getName())) {
                PlaywrightHighlighter.highlight(delegate);
            }
            Object result;
            try {
                result = method.invoke(delegate, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
            if (result instanceof Locator locatorResult) {
                return wrap(locatorResult);
            }
            if (result instanceof FrameLocator frameLocatorResult) {
                return HighlightingFrameLocator.wrap(frameLocatorResult);
            }
            return result;
        }
    }
}
