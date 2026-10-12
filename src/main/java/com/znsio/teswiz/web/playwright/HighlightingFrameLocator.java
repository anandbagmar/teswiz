package com.znsio.teswiz.web.playwright;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;

/**
 * A transparent {@link FrameLocator} that re-wraps any {@link Locator} it hands out in a
 * {@link HighlightingLocator}, so elements reached through an iframe highlight on action just like
 * elements on the top document. A {@code FrameLocator} has no action methods of its own — it only
 * produces locators — so this wrapper only needs to re-wrap results and forward everything else.
 */
final class HighlightingFrameLocator {

    private HighlightingFrameLocator() {
    }

    static FrameLocator wrap(FrameLocator delegate) {
        if (delegate instanceof HighlightingFrameMarker) {
            return delegate;
        }
        return (FrameLocator) Proxy.newProxyInstance(HighlightingFrameLocator.class.getClassLoader(),
                new Class<?>[] { FrameLocator.class, HighlightingFrameMarker.class },
                new HighlightingFrameHandler(delegate));
    }

    /** Marker so an already-wrapped frame locator is not wrapped twice. */
    private interface HighlightingFrameMarker {
    }

    private static final class HighlightingFrameHandler implements InvocationHandler {
        private final FrameLocator delegate;

        private HighlightingFrameHandler(FrameLocator delegate) {
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
                return wrap(frameLocatorResult);
            }
            return result;
        }
    }
}
