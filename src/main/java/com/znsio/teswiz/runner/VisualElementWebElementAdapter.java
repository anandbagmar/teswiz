package com.znsio.teswiz.runner;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

/**
 * Adapts a {@link VisualElement} to the Selenium {@link WebElement} API via a dynamic proxy, so a
 * visually-located element can be passed to code that expects a {@code WebElement}. Only a pragmatic subset of
 * {@code WebElement} is backed by real behaviour: {@code click} (with configured retry), {@code sendKeys},
 * {@code getText}/{@code getLocation}/{@code getSize}/{@code getRect} from the element's geometry/label. The
 * state queries ({@code isDisplayed}/{@code isEnabled}/{@code isSelected}) return fixed values because a
 * visual element has no DOM node to interrogate - callers should not treat these as real visibility checks.
 */
final class VisualElementWebElementAdapter implements InvocationHandler {
    private static final Logger LOGGER = LogManager.getLogger(VisualElementWebElementAdapter.class.getName());

    private final VisualElement visualElement;

    private VisualElementWebElementAdapter(VisualElement visualElement) {
        this.visualElement = visualElement;
    }

    static WebElement wrap(VisualElement visualElement) {
        return (WebElement) Proxy.newProxyInstance(
                VisualElementWebElementAdapter.class.getClassLoader(),
                new Class<?>[] { WebElement.class },
                new VisualElementWebElementAdapter(visualElement));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "click" -> {
                clickWithRetry();
                yield null;
            }
            case "sendKeys" -> {
                sendKeys(args);
                yield null;
            }
            case "isDisplayed", "isEnabled" -> true;
            case "isSelected" -> false;
            case "getText", "getAttribute" -> visualElement.getLabel();
            case "getTagName" -> "visual-element";
            case "getLocation" -> new Point(visualElement.getX(), visualElement.getY());
            case "getSize" -> visualElement.getSize();
            case "getRect" -> new Rectangle(visualElement.getX(), visualElement.getY(),
                    visualElement.getHeight(), visualElement.getWidth());
            case "toString" -> visualElement.toString();
            default -> null;
        };
    }

    private void clickWithRetry() {
        int retries = Math.max(1, Setup.getIntegerValueFromConfigs(Setup.VISUAL_ELEMENT_RETRY_ATTEMPTS));
        int delaySeconds = Math.max(1, Setup.getIntegerValueFromConfigs(Setup.VISUAL_ELEMENT_RETRY_DELAY_SECONDS));
        for (int attempt = 1; attempt <= retries; attempt++) {
            try {
                visualElement.click();
                return;
            } catch (RuntimeException e) {
                if (attempt == retries) {
                    throw e;
                }
                LOGGER.warn("Visual element click failed on attempt {} of {}, retrying after {}s: {}",
                        attempt, retries, delaySeconds, e.getMessage());
                sleepQuietly(Duration.ofSeconds(delaySeconds));
            }
        }
    }

    private void sendKeys(Object[] args) {
        if (args != null && args.length > 0 && args[0] instanceof CharSequence[] keys) {
            visualElement.sendKeys(keys);
        }
    }

    private static void sleepQuietly(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
