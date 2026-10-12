package com.znsio.teswiz.web.playwright;

import com.microsoft.playwright.Locator;
import com.znsio.teswiz.runner.Setup;

/**
 * The single Playwright-engine highlight implementation. Draws a transient outline + box-shadow on
 * an element via {@link Locator#evaluate(String)}, gated on the {@code HIGHLIGHT_ELEMENTS} config.
 *
 * <p>Both the {@code WebElement} adapter ({@link PlaywrightJavaWebElement}) and the transparent
 * action-highlighting {@link HighlightingLocator} delegate here so the highlight JS lives in one
 * place. The highlight stores the previous element's styles on {@code window.teswiz*} globals and
 * restores them when the next element is highlighted, so it does not permanently alter the page.
 * It never throws — a highlight must never block or fail the action it precedes.
 */
final class PlaywrightHighlighter {

    private static final String DEFAULT_COLOR = "#FF4500";
    private static final String DEFAULT_BORDER_WIDTH = "3px";

    private PlaywrightHighlighter() {
    }

    static void highlight(Locator locator) {
        if (!Setup.getBooleanValueFromConfigs(Setup.HIGHLIGHT_ELEMENTS)) {
            return;
        }
        try {
            String color = Setup.getStringValueFromConfigs(Setup.HIGHLIGHT_COLOR, DEFAULT_COLOR);
            String borderWidth = Setup.getStringValueFromConfigs(Setup.HIGHLIGHT_BORDER_WIDTH, DEFAULT_BORDER_WIDTH);
            locator.evaluate(highlightScript(color, borderWidth));
        } catch (RuntimeException ignored) {
            // A highlight is cosmetic; never let it block or fail the action it precedes.
        }
    }

    private static String highlightScript(String color, String borderWidth) {
        return "el => {" + "let visualBox = document.getElementById('teswiz-visual-highlight');"
                + "if (visualBox) { visualBox.remove(); }" + "if (window.teswizLastHighlightedElement) {" + "  try {"
                + "    window.teswizLastHighlightedElement.style.outline = window.teswizLastOutline || '';"
                + "    window.teswizLastHighlightedElement.style.outlineOffset = window.teswizLastOutlineOffset || '';"
                + "    window.teswizLastHighlightedElement.style.boxShadow = window.teswizLastBoxShadow || '';"
                + "  } catch(e) {}" + "}" + "window.teswizLastHighlightedElement = el;"
                + "window.teswizLastOutline = el.style.outline;"
                + "window.teswizLastOutlineOffset = el.style.outlineOffset;"
                + "window.teswizLastBoxShadow = el.style.boxShadow;" + "el.style.outline = '" + borderWidth + " solid "
                + color + "';" + "el.style.outlineOffset = '-2px';" + "el.style.boxShadow = '0 0 10px " + color + "';"
                + "}";
    }
}
