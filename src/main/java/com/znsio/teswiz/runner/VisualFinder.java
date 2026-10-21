package com.znsio.teswiz.runner;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import com.znsio.teswiz.entities.SpatialDirection;
import com.znsio.teswiz.entities.VisualRegion;
import com.znsio.teswiz.exceptions.NoSuchVisualElementException;
import com.znsio.teswiz.exceptions.VisualSubsystemDisabledException;
import com.znsio.teswiz.visual.ImageMatcher;
import com.znsio.teswiz.visual.LlmVisionEngine;
import com.znsio.teswiz.visual.OcrEngine;
import com.znsio.teswiz.visual.OcrReadHint;

/**
 * Owns the engine-agnostic OCR / image "find" surface previously embedded in {@link Visual}.
 *
 * <p>
 * It depends only on a screenshot source ({@code innerDriver}), the owning {@link Driver} facade (passed through to the
 * engines for coordinate scaling), and the injected {@link OcrEngine} / {@link ImageMatcher}. It does not touch any
 * Applitools state, which is why it can live independently of {@link Visual} and breaks the former
 * {@code Driver <-> Visual} find coupling. {@link Visual} keeps its public find methods and delegates them here for
 * compatibility.
 */
public class VisualFinder {
    private static final Logger LOGGER = LogManager.getLogger(VisualFinder.class.getName());

    private final WebDriver innerDriver;
    private final Driver driverFacade;
    private final OcrEngine ocrEngine;
    private final ImageMatcher imageMatcher;
    private final LlmVisionEngine llmVisionEngine;

    public VisualFinder(WebDriver innerDriver, Driver driverFacade, OcrEngine ocrEngine, ImageMatcher imageMatcher) {
        this(innerDriver, driverFacade, ocrEngine, imageMatcher, null);
    }

    public VisualFinder(WebDriver innerDriver, Driver driverFacade, OcrEngine ocrEngine, ImageMatcher imageMatcher,
            LlmVisionEngine llmVisionEngine) {
        this.innerDriver = innerDriver;
        this.driverFacade = driverFacade;
        this.ocrEngine = ocrEngine;
        this.imageMatcher = imageMatcher;
        this.llmVisionEngine = llmVisionEngine;
    }

    private int getVisualElementRetryAttempts() {
        int attempts = Setup.getIntegerValueFromConfigs(Setup.VISUAL_ELEMENT_RETRY_ATTEMPTS);
        return Math.max(1, attempts);
    }

    private int getVisualElementRetryDelayMs() {
        int delaySeconds = Setup.getIntegerValueFromConfigs(Setup.VISUAL_ELEMENT_RETRY_DELAY_SECONDS);
        return Math.max(1, delaySeconds) * 1000;
    }

    public VisualElement findByText(String text) {
        verifyOcrEnabled();
        LOGGER.info("Locating visual element by text '{}'", text);
        int maxAttempts = getVisualElementRetryAttempts();
        int retryDelayMs = getVisualElementRetryDelayMs();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            byte[] screenshot = captureScreenshotBytes();
            VisualElement match = this.ocrEngine.findTextMatch(screenshot, text, null, this.driverFacade);
            if (match != null) {
                return match;
            }
            if (attempt < maxAttempts) {
                LOGGER.info("Attempt {} of {}: Text '{}' not found via OCR, retrying after {}ms...", attempt,
                        maxAttempts, text, retryDelayMs);
                if (!sleepBetweenRetries(retryDelayMs)) {
                    break;
                }
            }
        }
        throw new NoSuchVisualElementException(String.format("Visual element with text '%s' not found via OCR.", text));
    }

    public VisualElement findByText(String text, VisualRegion region) {
        verifyOcrEnabled();
        String regionText = null != region ? region.toString() : "[entire screen]";
        LOGGER.info("Locating visual element by text '{}' within region {}", text, regionText);
        int maxAttempts = getVisualElementRetryAttempts();
        int retryDelayMs = getVisualElementRetryDelayMs();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            byte[] screenshot = captureScreenshotBytes();
            VisualElement match = this.ocrEngine.findTextMatch(screenshot, text, region, this.driverFacade);
            if (match != null) {
                return match;
            }
            if (attempt < maxAttempts) {
                LOGGER.info("Attempt {} of {}: Text '{}' not found via OCR in region {}, retrying after {}ms...",
                        attempt, maxAttempts, text, regionText, retryDelayMs);
                if (!sleepBetweenRetries(retryDelayMs)) {
                    break;
                }
            }
        }
        throw new NoSuchVisualElementException(
                String.format("Visual element with text '%s' not found via OCR in region %s.", text, regionText));
    }

    public VisualElement findByImage(List<String> imageTemplatePaths) {
        return findByImage(imageTemplatePaths, Runner.getVisualConfidenceThreshold());
    }

    public VisualElement findByImage(List<String> imageTemplatePaths, double confidenceThreshold) {
        return findByImage(imageTemplatePaths, confidenceThreshold, null);
    }

    public VisualElement findByImage(List<String> imageTemplatePaths, VisualRegion region) {
        return findByImage(imageTemplatePaths, Runner.getVisualConfidenceThreshold(), region);
    }

    public VisualElement findByImage(List<String> imageTemplatePaths, double confidenceThreshold, VisualRegion region) {
        verifyOcrEnabled();
        String regionText = null != region ? region.toString() : "[entire screen]";
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format(
                    "Locating visual element by candidate image templates %s with threshold %.2f within region %s",
                    imageTemplatePaths, confidenceThreshold, regionText));
        }
        int maxAttempts = getVisualElementRetryAttempts();
        int retryDelayMs = getVisualElementRetryDelayMs();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            byte[] screenshot = captureScreenshotBytes();
            VisualElement match = this.imageMatcher.findTemplateMatch(screenshot, imageTemplatePaths,
                    confidenceThreshold, region, this.driverFacade);
            if (match != null) {
                return match;
            }
            if (attempt < maxAttempts) {
                LOGGER.info("Attempt {} of {}: Image templates {} not matched in region {}, retrying after {}ms...",
                        attempt, maxAttempts, imageTemplatePaths, regionText, retryDelayMs);
                if (!sleepBetweenRetries(retryDelayMs)) {
                    break;
                }
            }
        }
        throw new NoSuchVisualElementException(String.format(
                "Visual element matching candidate image templates %s not found in region %s (confidence threshold: %.2f).",
                imageTemplatePaths, regionText, confidenceThreshold));
    }

    public VisualElement findByTextOrImage(String text, List<String> imageTemplatePaths) {
        return findByTextOrImage(text, imageTemplatePaths, null);
    }

    public VisualElement findByTextOrImage(String text, List<String> imageTemplatePaths, VisualRegion region) {
        verifyOcrEnabled();
        try {
            return findByText(text, region);
        } catch (NoSuchVisualElementException e) {
            String regionText = null != region ? region.toString() : "[entire screen]";
            LOGGER.info("Text '{}' not found via OCR in region {}. Falling back to candidate image templates {}", text,
                    regionText, imageTemplatePaths);
            return findByImage(imageTemplatePaths, region);
        }
    }

    public VisualElement findByImageOrText(List<String> imageTemplatePaths, String text) {
        verifyOcrEnabled();
        try {
            return findByImage(imageTemplatePaths);
        } catch (NoSuchVisualElementException e) {
            LOGGER.info("Candidate images {} not matched. Falling back to OCR text '{}'", imageTemplatePaths, text);
            return findByText(text);
        }
    }

    public VisualElement findByDescription(String description) {
        return findByDescription(description, (VisualRegion) null);
    }

    public VisualElement findByDescription(String description, VisualRegion region) {
        return findByDescription(description, null, region);
    }

    public VisualElement findByDescription(String description, List<String> referenceImagePaths) {
        return findByDescription(description, referenceImagePaths, null);
    }

    /**
     * Locates an element via the LLM vision fallback using any combination the caller supplies: a description
     * (OCR-style text), one or more reference images, or both. At least one of {@code description} /
     * {@code referenceImagePaths} must be non-empty.
     */
    public VisualElement findByDescription(String description, List<String> referenceImagePaths, VisualRegion region) {
        verifyLlmVisionEnabled();
        boolean hasDescription = description != null && !description.isBlank();
        boolean hasReferences = referenceImagePaths != null && !referenceImagePaths.isEmpty();
        if (!hasDescription && !hasReferences) {
            throw new NoSuchVisualElementException(
                    "LLM vision locate requires a description and/or at least one reference image, but neither was provided.");
        }

        String regionText = null != region ? region.toString() : "[entire screen]";
        LOGGER.info("Locating visual element via LLM (description='{}', referenceImages={}) within region {}",
                description, referenceImagePaths, regionText);
        int maxAttempts = getVisualElementRetryAttempts();
        int retryDelayMs = getVisualElementRetryDelayMs();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            byte[] screenshot = captureScreenshotBytes();
            VisualElement match = this.llmVisionEngine.findByDescription(screenshot, description, referenceImagePaths,
                    region, this.driverFacade);
            if (match != null) {
                return match;
            }
            if (attempt < maxAttempts) {
                LOGGER.info(
                        "Attempt {} of {}: element (description='{}', referenceImages={}) not located via LLM in region {}, retrying after {}ms...",
                        attempt, maxAttempts, description, referenceImagePaths, regionText, retryDelayMs);
                if (!sleepBetweenRetries(retryDelayMs)) {
                    break;
                }
            }
        }
        throw new NoSuchVisualElementException(String.format(
                "Visual element (description='%s', referenceImages=%s) not located via LLM vision in region %s.",
                description, referenceImagePaths, regionText));
    }

    /**
     * Reads (recognises) the text within {@code region} using OCR, tuned by {@code hint}. Unlike the finders, this
     * returns whatever is recognised rather than matching a supplied string. Returns an empty string when nothing is
     * read (never {@code null}).
     */
    public String readTextViaOcr(VisualRegion region, OcrReadHint hint) {
        verifyOcrEnabled();
        LOGGER.info("Reading text via OCR within region {} using {}", null != region ? region : "[entire screen]",
                hint);
        byte[] screenshot = captureScreenshotBytes();
        return this.ocrEngine.readText(screenshot, region, this.driverFacade, hint);
    }

    /**
     * Reads a numeric value within {@code region} using OCR and normalises it. Returns {@link Optional#empty()} when
     * the read is blank or not numeric; never throws for an unreadable region.
     */
    public Optional<BigDecimal> readNumberViaOcr(VisualRegion region) {
        verifyOcrEnabled();
        LOGGER.info("Reading number via OCR within region {}", null != region ? region : "[entire screen]");
        byte[] screenshot = captureScreenshotBytes();
        return this.ocrEngine.readNumber(screenshot, region, this.driverFacade);
    }

    /**
     * Reads the text the LLM vision model sees for {@code description} within {@code region}. Returns an empty string
     * when the model returns nothing (never {@code null}).
     */
    public String readTextViaLlm(String description, VisualRegion region) {
        verifyLlmVisionEnabled();
        LOGGER.info("Reading text via LLM (description='{}') within region {}", description,
                null != region ? region : "[entire screen]");
        byte[] screenshot = captureScreenshotBytes();
        return this.llmVisionEngine.readTextByDescription(screenshot, description, region, this.driverFacade);
    }

    public VisualElement findByTextOrImageOrDescription(String text, List<String> imageTemplatePaths,
            String description) {
        return findByTextOrImageOrDescription(text, imageTemplatePaths, description, null);
    }

    /**
     * OCR text -&gt; image templates -&gt; LLM fallback. When OCR and image matching both miss, the LLM tier receives
     * everything the caller provided: the OCR {@code text} is used as the description (an explicit {@code description}
     * overrides it when supplied) and the same {@code
     * imageTemplatePaths} are passed as reference images - so one image, many images, text, or any combination all flow
     * through to the model.
     */
    public VisualElement findByTextOrImageOrDescription(String text, List<String> imageTemplatePaths,
            String description, VisualRegion region) {
        verifyOcrEnabled();
        try {
            return findByText(text, region);
        } catch (NoSuchVisualElementException textMiss) {
            String regionText = null != region ? region.toString() : "[entire screen]";
            LOGGER.info("Text '{}' not found via OCR in region {}. Falling back to candidate image templates {}", text,
                    regionText, imageTemplatePaths);
            try {
                return findByImage(imageTemplatePaths, region);
            } catch (NoSuchVisualElementException imageMiss) {
                String effectiveDescription = (description != null && !description.isBlank()) ? description : text;
                LOGGER.info(
                        "Candidate images {} not matched in region {}. Falling back to LLM (description='{}', referenceImages={})",
                        imageTemplatePaths, regionText, effectiveDescription, imageTemplatePaths);
                return findByDescription(effectiveDescription, imageTemplatePaths, region);
            }
        }
    }

    public List<VisualElement> findAllByText(String text) {
        return findAllByText(text, (VisualRegion) null);
    }

    public List<VisualElement> findAllByText(String text, VisualRegion region) {
        verifyOcrEnabled();
        String regionText = null != region ? region.toString() : "[entire screen]";
        LOGGER.info("Locating all visual elements matching text '{}' within region {}", text, regionText);
        byte[] screenshot = captureScreenshotBytes();
        return this.ocrEngine.findAllTextMatches(screenshot, text, region, this.driverFacade);
    }

    public List<VisualElement> findAllByImage(List<String> imageTemplatePaths) {
        return findAllByImage(imageTemplatePaths, Runner.getVisualConfidenceThreshold());
    }

    public List<VisualElement> findAllByImage(List<String> imageTemplatePaths, double confidenceThreshold) {
        return findAllByImage(imageTemplatePaths, confidenceThreshold, null);
    }

    public List<VisualElement> findAllByImage(List<String> imageTemplatePaths, VisualRegion region) {
        return findAllByImage(imageTemplatePaths, Runner.getVisualConfidenceThreshold(), region);
    }

    public List<VisualElement> findAllByImage(List<String> imageTemplatePaths, double confidenceThreshold,
            VisualRegion region) {
        verifyOcrEnabled();
        String regionText = null != region ? region.toString() : "[entire screen]";
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format(
                    "Locating all visual elements matching image templates %s with threshold %.2f within region %s",
                    imageTemplatePaths, confidenceThreshold, regionText));
        }
        byte[] screenshot = captureScreenshotBytes();
        return this.imageMatcher.findAllTemplateMatches(screenshot, imageTemplatePaths, confidenceThreshold, region,
                this.driverFacade);
    }

    public List<VisualElement> findAllByTextOrImage(String text, List<String> imageTemplatePaths) {
        verifyOcrEnabled();
        List<VisualElement> textMatches = findAllByText(text);
        if (!textMatches.isEmpty()) {
            return textMatches;
        }
        return findAllByImage(imageTemplatePaths);
    }

    public List<VisualElement> findAllByImageOrText(List<String> imageTemplatePaths, String text) {
        verifyOcrEnabled();
        List<VisualElement> imageMatches = findAllByImage(imageTemplatePaths);
        if (!imageMatches.isEmpty()) {
            return imageMatches;
        }
        return findAllByText(text);
    }

    public VisualElement findRelativeByText(String targetText, SpatialDirection direction, String anchorText) {
        verifyOcrEnabled();
        LOGGER.info("Locating visual element '{}' {} anchor text '{}'", targetText, direction.getDirection(),
                anchorText);
        VisualElement anchor = findByText(anchorText);
        List<VisualElement> candidates = findAllByText(targetText);

        VisualElement bestCandidate = filterAndSelectClosestRelative(anchor, candidates, direction);
        if (bestCandidate != null) {
            return bestCandidate;
        }
        throw new NoSuchVisualElementException(String.format("Visual element with text '%s' not found %s anchor '%s'",
                targetText, direction.getDirection(), anchorText));
    }

    public VisualElement findRelativeByText(String targetText, SpatialDirection direction, VisualElement anchor) {
        verifyOcrEnabled();
        LOGGER.info("Locating visual element '{}' {} anchor element", targetText, direction.getDirection());
        List<VisualElement> candidates = findAllByText(targetText);

        VisualElement bestCandidate = filterAndSelectClosestRelative(anchor, candidates, direction);
        if (bestCandidate != null) {
            return bestCandidate;
        }
        throw new NoSuchVisualElementException(String.format(
                "Visual element with text '%s' not found %s anchor element", targetText, direction.getDirection()));
    }

    public VisualElement findRelativeByImage(List<String> targetImagePaths, SpatialDirection direction,
            String anchorText) {
        verifyOcrEnabled();
        LOGGER.info("Locating visual element matching images {} {} anchor text '{}'", targetImagePaths,
                direction.getDirection(), anchorText);
        VisualElement anchor = findByText(anchorText);
        List<VisualElement> candidates = findAllByImage(targetImagePaths);

        VisualElement bestCandidate = filterAndSelectClosestRelative(anchor, candidates, direction);
        if (bestCandidate != null) {
            return bestCandidate;
        }
        throw new NoSuchVisualElementException(
                String.format("Visual element matching images %s not found %s anchor '%s'", targetImagePaths,
                        direction.getDirection(), anchorText));
    }

    public VisualElement findRelativeByImage(List<String> targetImagePaths, SpatialDirection direction,
            VisualElement anchor) {
        verifyOcrEnabled();
        LOGGER.info("Locating visual element matching images {} {} anchor element", targetImagePaths,
                direction.getDirection());
        List<VisualElement> candidates = findAllByImage(targetImagePaths);

        VisualElement bestCandidate = filterAndSelectClosestRelative(anchor, candidates, direction);
        if (bestCandidate != null) {
            return bestCandidate;
        }
        throw new NoSuchVisualElementException(
                String.format("Visual element matching images %s not found %s anchor element", targetImagePaths,
                        direction.getDirection()));
    }

    private VisualElement filterAndSelectClosestRelative(VisualElement anchor, List<VisualElement> candidates,
            SpatialDirection direction) {
        if (anchor == null || candidates == null || candidates.isEmpty()) {
            return null;
        }

        int anchorCenterX = anchor.getCenter().getX();
        int anchorCenterY = anchor.getCenter().getY();
        VisualElement closest = null;
        double minDistance = Double.MAX_VALUE;

        for (VisualElement candidate : candidates) {
            int candidateCenterX = candidate.getCenter().getX();
            int candidateCenterY = candidate.getCenter().getY();

            if (direction.matchesRelativePosition(anchorCenterX, anchorCenterY, candidateCenterX, candidateCenterY)) {
                double distance = Math.hypot(candidateCenterX - anchorCenterX, candidateCenterY - anchorCenterY);
                if (distance < minDistance) {
                    minDistance = distance;
                    closest = candidate;
                }
            }
        }
        return closest;
    }

    private byte[] captureScreenshotBytes() {
        if (this.innerDriver instanceof TakesScreenshot) {
            return ((TakesScreenshot) this.innerDriver).getScreenshotAs(OutputType.BYTES);
        }
        LOGGER.warn("Inner driver does not implement TakesScreenshot.");
        return null;
    }

    /**
     * Sleeps for {@code retryDelayMs} between find attempts. Returns {@code false} and restores the thread's interrupt
     * flag if interrupted, signalling the caller to stop retrying.
     */
    private boolean sleepBetweenRetries(int retryDelayMs) {
        try {
            Thread.sleep(retryDelayMs);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void verifyLlmVisionEnabled() {
        if (this.llmVisionEngine == null || !Runner.isLlmVisionFallbackEnabled()) {
            throw new VisualSubsystemDisabledException(
                    "\n====================================================================================================\n"
                            + " [teswiz] LLM Vision Fallback is Disabled!\n"
                            + "----------------------------------------------------------------------------------------------------\n"
                            + " You invoked an LLM-based visual locator (e.g. driver.vision(VisionStrategy.LLM).findByText(...)), but\n"
                            + " LLM_VISION_FALLBACK_ENABLED=false in your execution configuration (teswiz_config.properties).\n\n"
                            + " To enable the LLM vision fallback:\n" + " 1. Set 'LLM_VISION_FALLBACK_ENABLED=true'.\n"
                            + " 2. Set 'LLM_VISION_ENDPOINT' and the API key env var named by 'LLM_VISION_API_KEY_ENV'.\n"
                            + "====================================================================================================\n");
        }
    }

    private void verifyOcrEnabled() {
        if (!Runner.isOcrEnabled()) {
            throw new VisualSubsystemDisabledException(
                    "\n====================================================================================================\n"
                            + " [teswiz] Visual OCR & Image Recognition Subsystem is Disabled!\n"
                            + "----------------------------------------------------------------------------------------------------\n"
                            + " You invoked a visual locator method (e.g. driver.vision(VisionStrategy.OCR).findByText(...)), but \n"
                            + " IS_OCR_ENABLED=false in your execution configuration (teswiz_config.properties).\n\n"
                            + " To enable OCR & Image Recognition capability in your project:\n"
                            + " 1. Set 'IS_OCR_ENABLED=true' in your teswiz_config.properties file or system property.\n"
                            + " 2. Ensure native visual dependencies (opencv, tess4j) are downloaded via ./gradlew downloadDependencies.\n"
                            + "====================================================================================================\n");
        }
    }
}
