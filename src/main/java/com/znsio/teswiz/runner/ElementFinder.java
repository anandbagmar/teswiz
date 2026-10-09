package com.znsio.teswiz.runner;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsElement;

/**
 * Decorates located {@link WebElement}s with a transparent proxy that highlights the element before
 * mutating interactions (click / sendKeys / clear / submit) and exposes the raw element via
 * {@code WrapsElement.getWrappedElement()}.
 *
 * <p>Extracted from {@link Driver} so the dynamic-proxy / decoration concern lives in one place.
 * It depends only on an {@link ElementHighlighter} (to draw the pre-action highlight) - the actual
 * {@code findElement}/{@code findElements} calls stay on {@link Driver} (which owns the driver and
 * its many locator variants) and route their results through {@link #decorate(WebElement)} /
 * {@link #decorate(List)}. Playwright elements and already-decorated proxies are returned as-is.
 */
final class ElementFinder {

    private final ElementHighlighter elementHighlighter;

    ElementFinder(ElementHighlighter elementHighlighter) {
        this.elementHighlighter = elementHighlighter;
    }

    WebElement decorate(WebElement element) {
        if (element == null) {
            return null;
        }
        if (Proxy.isProxyClass(element.getClass())
                && Proxy.getInvocationHandler(element) instanceof ElementInvocationHandler) {
            return element;
        }
        if (element.getClass().getName().contains("Playwright")) {
            return element;
        }
        return (WebElement) Proxy.newProxyInstance(ElementFinder.class.getClassLoader(),
                new Class<?>[] { WebElement.class, WrapsElement.class }, new ElementInvocationHandler(element));
    }

    List<WebElement> decorate(List<WebElement> elements) {
        if (elements == null) {
            return Collections.emptyList();
        }
        return elements.stream().map(this::decorate).collect(Collectors.toList());
    }

    private class ElementInvocationHandler implements InvocationHandler {
        private final WebElement target;

        ElementInvocationHandler(WebElement target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if ("getWrappedElement".equals(methodName) && (args == null || args.length == 0)) {
                return target;
            }
            if ("click".equals(methodName) || "sendKeys".equals(methodName) || "clear".equals(methodName)
                    || "submit".equals(methodName)) {
                elementHighlighter.highlightElement(target);
            }
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}
