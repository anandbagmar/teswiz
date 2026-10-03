package com.znsio.teswiz.businessLayer.visualocr;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.Direction;
import com.znsio.teswiz.entities.Platform;
import com.znsio.teswiz.entities.SpatialDirection;
import com.znsio.teswiz.exceptions.VisualSubsystemDisabledException;
import com.znsio.teswiz.runner.Runner;
import com.znsio.teswiz.runner.VisualElement;
import com.znsio.teswiz.screen.visualocr.VisualOcrScreen;

public class VisualOcrBL {
    private static final Logger LOGGER = LogManager.getLogger(VisualOcrBL.class.getName());
    private final TestExecutionContext context;
    private final String currentUserPersona;
    private final Platform currentPlatform;

    public VisualOcrBL(String userPersona, Platform forPlatform) {
        long threadId = Thread.currentThread().getId();
        this.context = Runner.getTestExecutionContext(threadId);
        this.currentUserPersona = userPersona;
        this.currentPlatform = forPlatform;
        Runner.setCurrentDriverForUser(userPersona, forPlatform, context);
    }

    public VisualOcrBL() {
        this.context = null;
        this.currentUserPersona = "DEFAULT";
        this.currentPlatform = Platform.web;
    }

    // --- Query / Finder Methods (Delegates to VisualOcrScreen.get()) ---

    public VisualElement findVisualElementByText(String ocrText) {
        return VisualOcrScreen.get().findVisualElementByText(ocrText);
    }

    public VisualElement findVisualElementByImage(String imageTemplatePath) {
        return VisualOcrScreen.get().findVisualElementByImage(List.of(imageTemplatePath));
    }

    public VisualElement findVisualElementByTextOrImage(String ocrText, String imageTemplatePath) {
        return VisualOcrScreen.get().findVisualElementByTextOrImage(ocrText, List.of(imageTemplatePath));
    }

    public VisualElement findVisualElementByImageOrText(String imageTemplatePath, String ocrText) {
        return VisualOcrScreen.get().findVisualElementByImageOrText(List.of(imageTemplatePath), ocrText);
    }

    // --- Pure Verification Methods ---

    public VisualOcrBL verifyVisualElementIsPresentByText(String elementName, String ocrText) {
        LOGGER.info(String.format("Verifying visual element '%s' is present using OCR text '%s'", elementName, ocrText));
        VisualElement element = VisualOcrScreen.get().findVisualElementByText(ocrText);
        assertThat(element).as("Visual element '" + elementName + "' with OCR text '" + ocrText + "' should be found").isNotNull();
        return this;
    }

    public VisualOcrBL verifyVisualElementIsNotPresentByText(String elementName, String ocrText) {
        LOGGER.info(String.format("Verifying visual element '%s' is NOT present using OCR text '%s'", elementName, ocrText));
        VisualElement element = VisualOcrScreen.get().findVisualElementByText(ocrText);
        assertThat(element).as("Visual element '" + elementName + "' with OCR text '" + ocrText + "' should NOT be present").isNull();
        return this;
    }

    public VisualOcrBL verifyVisualElementIsPresentByImage(String elementName, String imageTemplatePath) {
        LOGGER.info(String.format("Verifying visual element '%s' is present using image template '%s'", elementName, imageTemplatePath));
        VisualElement element = VisualOcrScreen.get().findVisualElementByImage(List.of(imageTemplatePath));
        assertThat(element).as("Visual element '" + elementName + "' matched by image template '" + imageTemplatePath + "' should be found").isNotNull();
        return this;
    }

    public VisualOcrBL verifyVisualElementIsNotPresentByImage(String elementName, String imageTemplatePath) {
        LOGGER.info(String.format("Verifying visual element '%s' is NOT present using image template '%s'", elementName, imageTemplatePath));
        VisualElement element = VisualOcrScreen.get().findVisualElementByImage(List.of(imageTemplatePath));
        assertThat(element).as("Visual element '" + elementName + "' matched by image template '" + imageTemplatePath + "' should NOT be present").isNull();
        return this;
    }

    public VisualOcrBL verifyVisualElementIsPresentByFallbackTextOrImage(String elementName, String ocrText, String imageTemplatePath) {
        LOGGER.info(String.format("Verifying visual element '%s' is present using fallback OCR text '%s' or image template '%s'", elementName, ocrText, imageTemplatePath));
        VisualElement element = VisualOcrScreen.get().findVisualElementByTextOrImage(ocrText, List.of(imageTemplatePath));
        assertThat(element).as("Visual element '" + elementName + "' matched by text/image should be found").isNotNull();
        return this;
    }

    public VisualOcrBL verifyVisualElementIsPresentByFallbackImageOrText(String elementName, String imageTemplatePath, String ocrText) {
        LOGGER.info(String.format("Verifying visual element '%s' is present using fallback image template '%s' or OCR text '%s'", elementName, imageTemplatePath, ocrText));
        VisualElement element = VisualOcrScreen.get().findVisualElementByImageOrText(List.of(imageTemplatePath), ocrText);
        assertThat(element).as("Visual element '" + elementName + "' matched by image/text should be found").isNotNull();
        return this;
    }

    // --- Conditional Action Methods (Delegates strictly to VisualOcrScreen.get()) ---

    public boolean tryVisuallyClickUsingOcrText(String elementName, String ocrText) {
        return VisualOcrScreen.get().tryClickVisualElementByText(elementName, ocrText);
    }

    public boolean tryVisuallyClickUsingImageTemplate(String elementName, String imageTemplatePath) {
        return VisualOcrScreen.get().tryClickVisualElementByImage(elementName, List.of(imageTemplatePath));
    }

    public boolean tryVisuallyClickUsingFallbackOcrTextOrImageTemplate(String elementName, String ocrText, String imageTemplatePath) {
        return VisualOcrScreen.get().tryClickVisualElementByTextOrImage(elementName, ocrText, List.of(imageTemplatePath));
    }

    public boolean tryVisuallyClickUsingFallbackImageTemplateOrOcrText(String elementName, String imageTemplatePath, String ocrText) {
        return VisualOcrScreen.get().tryClickVisualElementByImageOrText(elementName, List.of(imageTemplatePath), ocrText);
    }

    // --- Strict Action Methods (Delegates strictly to VisualOcrScreen.get()) ---

    public VisualOcrBL visuallyClickUsingImageTemplate(String elementName, String imageTemplatePath) {
        VisualOcrScreen.get().clickVisualElementByImage(elementName, List.of(imageTemplatePath));
        return this;
    }

    public VisualOcrBL visuallyClickUsingOcrText(String elementName, String ocrText) {
        VisualOcrScreen.get().clickVisualElementByText(elementName, ocrText);
        return this;
    }

    public VisualOcrBL visuallyEnterTextUsingOcrText(String elementName, String textToEnter, String ocrText) {
        VisualOcrScreen.get().enterTextIntoVisualElementByText(elementName, textToEnter, ocrText);
        return this;
    }

    public VisualOcrBL visuallyDoubleClickUsingOcrText(String elementName, String ocrText) {
        VisualOcrScreen.get().doubleClickVisualElementByText(elementName, ocrText);
        return this;
    }

    public VisualOcrBL visuallyHoverUsingOcrText(String elementName, String ocrText) {
        VisualOcrScreen.get().hoverVisualElementByText(elementName, ocrText);
        return this;
    }

    public VisualOcrBL visuallyLongPressUsingOcrText(String elementName, String ocrText) {
        VisualOcrScreen.get().longPressVisualElementByText(elementName, ocrText);
        return this;
    }

    public VisualOcrBL visuallySwipeOnElementUsingOcrText(String elementName, String directionText, String ocrText) {
        VisualOcrScreen.get().swipeOnVisualElementByText(elementName, Direction.valueOf(directionText.toUpperCase()), ocrText);
        return this;
    }

    public VisualOcrBL visuallyInspectUsingOcrText(String elementName, String ocrText) {
        VisualOcrScreen.get().inspectVisualElementByText(elementName, ocrText);
        return this;
    }

    public VisualOcrBL visuallyInspectUsingImageTemplate(String elementName, String imageTemplatePath) {
        VisualOcrScreen.get().inspectVisualElementByImage(elementName, List.of(imageTemplatePath));
        return this;
    }

    public VisualOcrBL visuallyClickUsingFallbackOcrTextOrImageTemplate(String elementName, String ocrText, String imageTemplatePath) {
        VisualOcrScreen.get().clickVisualElementByTextOrImage(elementName, ocrText, List.of(imageTemplatePath));
        return this;
    }

    public VisualOcrBL visuallyClickUsingFallbackImageTemplateOrOcrText(String elementName, String imageTemplatePath, String ocrText) {
        VisualOcrScreen.get().clickVisualElementByImageOrText(elementName, List.of(imageTemplatePath), ocrText);
        return this;
    }

    // --- Subsystem Disabled Verification Methods ---

    public VisualOcrBL verifyVisualOcrCapabilityDisabled() {
        assertThat(Runner.isOcrEnabled()).isFalse();
        LOGGER.info("Verified IS_OCR_ENABLED is set to false");
        return this;
    }

    public VisualOcrBL verifyOcrDisabledThrowsExceptionForText(String text) {
        LOGGER.info("Verifying findByText throws VisualSubsystemDisabledException for text: " + text);
        assertThat(Runner.isOcrEnabled()).isFalse();
        assertThatThrownBy(() -> VisualOcrScreen.get().findVisualElementByText(text))
                .isInstanceOf(VisualSubsystemDisabledException.class)
                .hasMessageContaining("[teswiz] Visual OCR & Image Recognition Subsystem is Disabled!");
        return this;
    }

    public VisualOcrBL verifyOcrDisabledThrowsExceptionForImage(String imagePath) {
        LOGGER.info("Verifying findByImage throws VisualSubsystemDisabledException for image: " + imagePath);
        assertThat(Runner.isOcrEnabled()).isFalse();
        assertThatThrownBy(() -> VisualOcrScreen.get().findVisualElementByImage(List.of(imagePath)))
                .isInstanceOf(VisualSubsystemDisabledException.class)
                .hasMessageContaining("[teswiz] Visual OCR & Image Recognition Subsystem is Disabled!");
        return this;
    }

    public VisualOcrBL verifyOcrDisabledThrowsExceptionForTextOrImage(String text, String imagePath) {
        LOGGER.info("Verifying findByTextOrImage throws VisualSubsystemDisabledException for text: " + text + ", image: " + imagePath);
        assertThat(Runner.isOcrEnabled()).isFalse();
        assertThatThrownBy(() -> VisualOcrScreen.get().findVisualElementByTextOrImage(text, List.of(imagePath)))
                .isInstanceOf(VisualSubsystemDisabledException.class)
                .hasMessageContaining("[teswiz] Visual OCR & Image Recognition Subsystem is Disabled!");
        return this;
    }

    public VisualOcrBL verifyOcrDisabledThrowsExceptionForImageOrText(String imagePath, String text) {
        LOGGER.info("Verifying findByImageOrText throws VisualSubsystemDisabledException for image: " + imagePath + ", text: " + text);
        assertThat(Runner.isOcrEnabled()).isFalse();
        assertThatThrownBy(() -> VisualOcrScreen.get().findVisualElementByImageOrText(List.of(imagePath), text))
                .isInstanceOf(VisualSubsystemDisabledException.class)
                .hasMessageContaining("[teswiz] Visual OCR & Image Recognition Subsystem is Disabled!");
        return this;
    }

    public List<VisualElement> findAllVisualElementsByText(String ocrText) {
        return VisualOcrScreen.get().findAllVisualElementsByText(ocrText);
    }

    public List<VisualElement> findAllVisualElementsByImage(String imageTemplatePath) {
        return VisualOcrScreen.get().findAllVisualElementsByImage(List.of(imageTemplatePath));
    }

    public List<VisualElement> findAllVisualElementsByTextOrImage(String ocrText, String imageTemplatePath) {
        return VisualOcrScreen.get().findAllVisualElementsByTextOrImage(ocrText, List.of(imageTemplatePath));
    }

    public List<VisualElement> findAllVisualElementsByImageOrText(String imageTemplatePath, String ocrText) {
        return VisualOcrScreen.get().findAllVisualElementsByImageOrText(List.of(imageTemplatePath), ocrText);
    }

    private List<VisualElement> pollUntilAtLeastN(java.util.function.Supplier<List<VisualElement>> query, int minCount) {
        int maxAttempts = Math.max(1, com.znsio.teswiz.runner.Setup.getIntegerValueFromConfigs(com.znsio.teswiz.runner.Setup.VISUAL_ELEMENT_RETRY_ATTEMPTS));
        int retryDelayMs = Math.max(1, com.znsio.teswiz.runner.Setup.getIntegerValueFromConfigs(com.znsio.teswiz.runner.Setup.VISUAL_ELEMENT_RETRY_DELAY_SECONDS)) * 1000;
        List<VisualElement> elements = java.util.Collections.emptyList();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            elements = query.get();
            if (elements.size() >= minCount) {
                return elements;
            }
            if (attempt < maxAttempts) {
                try { Thread.sleep(retryDelayMs); } catch (InterruptedException ignored) {}
            }
        }
        return elements;
    }

    private List<VisualElement> pollUntilExactCount(java.util.function.Supplier<List<VisualElement>> query, int expectedCount) {
        int maxAttempts = Math.max(1, com.znsio.teswiz.runner.Setup.getIntegerValueFromConfigs(com.znsio.teswiz.runner.Setup.VISUAL_ELEMENT_RETRY_ATTEMPTS));
        int retryDelayMs = Math.max(1, com.znsio.teswiz.runner.Setup.getIntegerValueFromConfigs(com.znsio.teswiz.runner.Setup.VISUAL_ELEMENT_RETRY_DELAY_SECONDS)) * 1000;
        List<VisualElement> elements = java.util.Collections.emptyList();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            elements = query.get();
            if (elements.size() == expectedCount) {
                return elements;
            }
            if (attempt < maxAttempts) {
                try { Thread.sleep(retryDelayMs); } catch (InterruptedException ignored) {}
            }
        }
        return elements;
    }

    public VisualOcrBL verifyVisualElementCountByText(String elementName, String ocrText, int expectedCount) {
        LOGGER.info(String.format("Verifying %d visual elements named '%s' present using OCR text '%s'", expectedCount, elementName, ocrText));
        List<VisualElement> elements = pollUntilExactCount(() -> findAllVisualElementsByText(ocrText), expectedCount);
        assertThat(elements)
                .as("Expected " + expectedCount + " visual elements matching OCR text '" + ocrText + "'")
                .hasSize(expectedCount);
        return this;
    }

    public VisualOcrBL verifyVisualElementCountByImage(String elementName, String imageTemplatePath, int expectedCount) {
        LOGGER.info(String.format("Verifying %d visual elements named '%s' present using image template '%s'", expectedCount, elementName, imageTemplatePath));
        List<VisualElement> elements = pollUntilExactCount(() -> findAllVisualElementsByImage(imageTemplatePath), expectedCount);
        assertThat(elements)
                .as("Expected " + expectedCount + " visual elements matching image template '" + imageTemplatePath + "'")
                .hasSize(expectedCount);
        return this;
    }

    public VisualOcrBL verifyAtLeastNVisualElementsPresentByText(String elementName, String ocrText, int minCount) {
        LOGGER.info(String.format("Verifying at least %d visual elements named '%s' present using OCR text '%s'", minCount, elementName, ocrText));
        List<VisualElement> elements = pollUntilAtLeastN(() -> findAllVisualElementsByText(ocrText), minCount);
        assertThat(elements)
                .as("Expected at least " + minCount + " visual elements matching OCR text '" + ocrText + "'")
                .hasSizeGreaterThanOrEqualTo(minCount);
        return this;
    }

    public VisualOcrBL verifyAtLeastNVisualElementsPresentByImage(String elementName, String imageTemplatePath, int minCount) {
        LOGGER.info(String.format("Verifying at least %d visual elements named '%s' present using image template '%s'", minCount, elementName, imageTemplatePath));
        List<VisualElement> elements = pollUntilAtLeastN(() -> findAllVisualElementsByImage(imageTemplatePath), minCount);
        assertThat(elements)
                .as("Expected at least " + minCount + " visual elements matching image template '" + imageTemplatePath + "'")
                .hasSizeGreaterThanOrEqualTo(minCount);
        return this;
    }

    public VisualOcrBL visuallyFindAllInstancesUsingImageTemplate(String elementName, String imageTemplatePath) {
        LOGGER.info(String.format("Visually finding all instances of '%s' using image template '%s'", elementName, imageTemplatePath));
        VisualOcrScreen.get().inspectVisualElementByImage(elementName, List.of(imageTemplatePath));
        return this;
    }

    public VisualOcrBL visuallyFindAllInstancesUsingOcrText(String elementName, String ocrText) {
        LOGGER.info(String.format("Visually finding all instances of '%s' using OCR text '%s'", elementName, ocrText));
        VisualOcrScreen.get().inspectVisualElementByText(elementName, ocrText);
        return this;
    }

    public VisualOcrBL visuallyFindAllInstancesUsingFallbackOcrTextOrImageTemplate(String elementName, String ocrText, String imageTemplatePath) {
        LOGGER.info(String.format("Visually finding all instances of '%s' using fallback OCR text '%s' or image template '%s'", elementName, ocrText, imageTemplatePath));
        VisualOcrScreen.get().clickVisualElementByTextOrImage(elementName, ocrText, List.of(imageTemplatePath));
        return this;
    }

    public VisualOcrBL visuallyFindAllInstancesUsingFallbackImageTemplateOrOcrText(String elementName, String imageTemplatePath, String ocrText) {
        LOGGER.info(String.format("Visually finding all instances of '%s' using fallback image template '%s' or OCR text '%s'", elementName, imageTemplatePath, ocrText));
        VisualOcrScreen.get().clickVisualElementByImageOrText(elementName, List.of(imageTemplatePath), ocrText);
        return this;
    }

    public VisualOcrBL visuallyClickElementAtIndexUsingOcrText(String elementName, int index, String ocrText) {
        VisualOcrScreen.get().clickVisualElementAtIndexByText(elementName, index, ocrText);
        return this;
    }

    public VisualOcrBL visuallyClickElementByPositionUsingOcrText(String elementName, String positionText, String ocrText) {
        VisualOcrScreen.get().clickVisualElementByPositionByText(elementName, positionText, ocrText);
        return this;
    }

    public VisualOcrBL visuallyClickElementByPositionUsingImageTemplate(String elementName, String positionText, String imageTemplatePath) {
        VisualOcrScreen.get().clickVisualElementByPositionByImage(elementName, positionText, List.of(imageTemplatePath));
        return this;
    }

    public VisualOcrBL visuallyClickUsingOcrTextRelative(String elementName, String targetText, String directionText, String anchorText) {
        VisualOcrScreen.get().clickVisualElementRelativeByText(elementName, targetText, SpatialDirection.fromString(directionText), anchorText);
        return this;
    }

    public boolean tryVisuallyClickUsingOcrTextRelative(String elementName, String targetText, String directionText, String anchorText) {
        return VisualOcrScreen.get().tryClickVisualElementRelativeByText(elementName, targetText, SpatialDirection.fromString(directionText), anchorText);
    }

    public VisualOcrBL verifyVisualElementIsPresentRelativeByText(String elementName, String targetText, String directionText, String anchorText) {
        LOGGER.info(String.format("Verifying visual element '%s' with OCR text '%s' present %s anchor text '%s'", elementName, targetText, directionText, anchorText));
        VisualElement element = VisualOcrScreen.get().findVisualElementRelativeByText(targetText, SpatialDirection.fromString(directionText), anchorText);
        assertThat(element).as("Visual element '" + elementName + "' relative to '" + anchorText + "' should be found").isNotNull();
        return this;
    }

    public VisualOcrBL verifyVisualElementIsPresentInRegion(String elementName, String ocrText, int x, int y, int width, int height) {
        com.znsio.teswiz.entities.VisualRegion region = com.znsio.teswiz.entities.VisualRegion.inRegion(x, y, width, height);
        LOGGER.info(String.format("Verifying visual element '%s' with OCR text '%s' is present in region %s", elementName, ocrText, region));
        VisualElement element = VisualOcrScreen.get().findVisualElementByTextInRegion(ocrText, region);
        assertThat(element).as("Visual element '" + elementName + "' in region " + region + " should be present").isNotNull();
        return this;
    }

    public int parsePositionIndex(String positionText, int listSize) {
        if (positionText == null || positionText.isBlank()) return 0;
        String normalized = positionText.trim().toLowerCase();
        if ("first".equals(normalized) || "1st".equals(normalized)) return 0;
        if ("last".equals(normalized)) return Math.max(0, listSize - 1);
        if ("second".equals(normalized) || "2nd".equals(normalized)) return 1;
        if ("third".equals(normalized) || "3rd".equals(normalized)) return 2;
        if (normalized.endsWith("st") || normalized.endsWith("nd") || normalized.endsWith("rd") || normalized.endsWith("th")) {
            String digits = normalized.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) return Integer.parseInt(digits) - 1;
        }
        try { return Integer.parseInt(normalized); } catch (NumberFormatException e) { return 0; }
    }
}
