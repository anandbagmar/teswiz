package com.znsio.teswiz.runner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Draws and clears the visual highlight overlay used when acting on an element.
 *
 * <p>Extracted from {@link Driver} so the DOM/JS highlight concern (and the {@code activeHighlightBounds}
 * state) lives in one place instead of being interleaved with gesture/find logic. Highlighting is a
 * web-only concern - on native mobile (Appium) the DOM-overlay methods no-op (the screenshot-canvas
 * highlighter handles mobile), so parity with web is intentionally not provided here.
 *
 * <p>{@link Driver} keeps thin public {@code highlightElement}/{@code highlightVisualElement}/
 * {@code clearHighlight}/{@code getActiveHighlightBounds} methods delegating here, so callers
 * ({@code VisualElement}, the element proxy) are unaffected.
 */
final class ElementHighlighter {

    private static final Logger LOGGER = LogManager.getLogger(ElementHighlighter.class.getName());

    private final WebDriver driver;
    private final boolean nativeMobile;
    private Rectangle activeHighlightBounds;

    ElementHighlighter(WebDriver driver, boolean nativeMobile) {
        this.driver = driver;
        this.nativeMobile = nativeMobile;
    }

    Rectangle getActiveHighlightBounds() {
        return activeHighlightBounds;
    }

    void clearHighlight() {
        this.activeHighlightBounds = null;
        if (!Setup.getBooleanValueFromConfigs(Setup.HIGHLIGHT_ELEMENTS)) {
            return;
        }
        if (nativeMobile) {
            return;
        }
        if (driver instanceof JavascriptExecutor js) {
            try {
                js.executeScript(
                    "let visualBox = document.getElementById('teswiz-visual-highlight');" +
                    "if (visualBox) { visualBox.remove(); }" +
                    "if (window.teswizLastHighlightedElement) {" +
                    "  try {" +
                    "    window.teswizLastHighlightedElement.style.outline = window.teswizLastOutline || '';" +
                    "    window.teswizLastHighlightedElement.style.outlineOffset = window.teswizLastOutlineOffset || '';" +
                    "    window.teswizLastHighlightedElement.style.boxShadow = window.teswizLastBoxShadow || '';" +
                    "  } catch(e) {}" +
                    "  delete window.teswizLastHighlightedElement;" +
                    "  delete window.teswizLastOutline;" +
                    "  delete window.teswizLastOutlineOffset;" +
                    "  delete window.teswizLastBoxShadow;" +
                    "}"
                );
            } catch (Exception ignored) {}
        }
    }

    void highlightElement(WebElement element) {
        if (!Setup.getBooleanValueFromConfigs(Setup.HIGHLIGHT_ELEMENTS)) {
            return;
        }
        clearHighlight();
        if (nativeMobile) {
            LOGGER.debug("DOM-based element highlighting is not supported on native mobile app.");
            return;
        }
        String color = Setup.getStringValueFromConfigs(Setup.HIGHLIGHT_COLOR, "#FF4500");
        String borderWidth = Setup.getStringValueFromConfigs(Setup.HIGHLIGHT_BORDER_WIDTH, "3px");
        if (driver instanceof JavascriptExecutor js) {
            try {
                js.executeScript(
                    "window.teswizLastHighlightedElement = arguments[0];" +
                    "window.teswizLastOutline = arguments[0].style.outline;" +
                    "window.teswizLastOutlineOffset = arguments[0].style.outlineOffset;" +
                    "window.teswizLastBoxShadow = arguments[0].style.boxShadow;" +
                    "arguments[0].style.outline = '" + borderWidth + " solid " + color + "';" +
                    "arguments[0].style.outlineOffset = '-2px';" +
                    "arguments[0].style.boxShadow = '0 0 10px " + color + "';"
                , element);
                LOGGER.info("Highlighted WebElement visually with outline color: " + color);
            } catch (Exception e) {
                LOGGER.debug("Could not highlight web element: " + e.getMessage());
            }
        }
    }

    void highlightVisualElement(int x, int y, int width, int height) {
        if (!Setup.getBooleanValueFromConfigs(Setup.HIGHLIGHT_ELEMENTS)) {
            return;
        }
        clearHighlight();
        this.activeHighlightBounds = new Rectangle(x, y, height, width);
        if (nativeMobile) {
            LOGGER.info("Visual element screenshot image canvas highlighting active on native mobile app at bounds [x={}, y={}, w={}, h={}]", x, y, width, height);
            return;
        }
        String color = Setup.getStringValueFromConfigs(Setup.HIGHLIGHT_COLOR, "#FF4500");
        String borderWidth = Setup.getStringValueFromConfigs(Setup.HIGHLIGHT_BORDER_WIDTH, "3px");
        if (driver instanceof JavascriptExecutor js) {
            try {
                js.executeScript(
                    "let id = 'teswiz-visual-highlight';" +
                    "let box = document.createElement('div');" +
                    "box.id = id;" +
                    "document.body.appendChild(box);" +
                    "box.style.position = 'fixed';" +
                    "box.style.left = '" + x + "px';" +
                    "box.style.top = '" + y + "px';" +
                    "box.style.width = '" + width + "px';" +
                    "box.style.height = '" + height + "px';" +
                    "box.style.border = '" + borderWidth + " solid " + color + "';" +
                    "box.style.backgroundColor = 'rgba(255, 69, 0, 0.25)';" +
                    "box.style.boxShadow = '0 0 10px " + color + "';" +
                    "box.style.zIndex = '2147483647';" +
                    "box.style.pointerEvents = 'none';" +
                    "box.style.boxSizing = 'border-box';" +
                    "box.style.transition = 'all 0.1s ease-in-out';"
                );
                LOGGER.info("Highlighted visual element at viewport bounds [x={}, y={}, w={}, h={}] with color {}", x, y, width, height, color);
            } catch (Exception e) {
                LOGGER.debug("Could not highlight visual element at (" + x + ", " + y + "): " + e.getMessage());
            }
        }
    }

    void highlightVisualElement(VisualElement visualElement) {
        if (visualElement != null) {
            highlightVisualElement(visualElement.getX(), visualElement.getY(), visualElement.getWidth(), visualElement.getHeight());
        }
    }
}
