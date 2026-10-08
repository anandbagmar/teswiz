package com.znsio.teswiz.runner;

import java.util.List;

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
import com.znsio.teswiz.visual.OcrEngine;

/**
 * Owns the engine-agnostic OCR / image "find" surface previously embedded in {@link Visual}.
 *
 * <p>It depends only on a screenshot source ({@code innerDriver}), the owning {@link Driver} facade
 * (passed through to the engines for coordinate scaling), and the injected {@link OcrEngine} /
 * {@link ImageMatcher}. It does not touch any Applitools state, which is why it can live
 * independently of {@link Visual} and breaks the former {@code Driver <-> Visual} find coupling.
 * {@link Visual} keeps its public find methods and delegates them here for compatibility.
 */
public class VisualFinder {
    private static final Logger LOGGER = LogManager.getLogger(VisualFinder.class.getName());

    private final WebDriver innerDriver;
    private final Driver driverFacade;
    private final OcrEngine ocrEngine;
    private final ImageMatcher imageMatcher;

    public VisualFinder(WebDriver innerDriver, Driver driverFacade, OcrEngine ocrEngine, ImageMatcher imageMatcher) {
        this.innerDriver = innerDriver;
        this.driverFacade = driverFacade;
        this.ocrEngine = ocrEngine;
        this.imageMatcher = imageMatcher;
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
                LOGGER.info("Attempt {} of {}: Text '{}' not found via OCR, retrying after {}ms...", attempt, maxAttempts, text, retryDelayMs);
                if (!sleepBetweenRetries(retryDelayMs)) {
                    break;
                }
            }
        }
        throw new NoSuchVisualElementException(
                String.format("Visual element with text '%s' not found via OCR.", text));
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
                LOGGER.info("Attempt {} of {}: Text '{}' not found via OCR in region {}, retrying after {}ms...", attempt, maxAttempts, text, regionText, retryDelayMs);
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
            LOGGER.info(String.format("Locating visual element by candidate image templates %s with threshold %.2f within region %s", imageTemplatePaths, confidenceThreshold, regionText));
        }
        int maxAttempts = getVisualElementRetryAttempts();
        int retryDelayMs = getVisualElementRetryDelayMs();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            byte[] screenshot = captureScreenshotBytes();
            VisualElement match = this.imageMatcher.findTemplateMatch(screenshot, imageTemplatePaths, confidenceThreshold, region, this.driverFacade);
            if (match != null) {
                return match;
            }
            if (attempt < maxAttempts) {
                LOGGER.info("Attempt {} of {}: Image templates {} not matched in region {}, retrying after {}ms...", attempt, maxAttempts, imageTemplatePaths, regionText, retryDelayMs);
                if (!sleepBetweenRetries(retryDelayMs)) {
                    break;
                }
            }
        }
        throw new NoSuchVisualElementException(
                String.format("Visual element matching candidate image templates %s not found in region %s (confidence threshold: %.2f).", imageTemplatePaths, regionText, confidenceThreshold));
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
            LOGGER.info("Text '{}' not found via OCR in region {}. Falling back to candidate image templates {}", text, regionText, imageTemplatePaths);
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

    public List<VisualElement> findAllByImage(List<String> imageTemplatePaths, double confidenceThreshold, VisualRegion region) {
        verifyOcrEnabled();
        String regionText = null != region ? region.toString() : "[entire screen]";
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Locating all visual elements matching image templates %s with threshold %.2f within region %s", imageTemplatePaths, confidenceThreshold, regionText));
        }
        byte[] screenshot = captureScreenshotBytes();
        return this.imageMatcher.findAllTemplateMatches(screenshot, imageTemplatePaths, confidenceThreshold, region, this.driverFacade);
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
        LOGGER.info("Locating visual element '{}' {} anchor text '{}'", targetText, direction.getDirection(), anchorText);
        VisualElement anchor = findByText(anchorText);
        List<VisualElement> candidates = findAllByText(targetText);

        VisualElement bestCandidate = filterAndSelectClosestRelative(anchor, candidates, direction);
        if (bestCandidate != null) {
            return bestCandidate;
        }
        throw new NoSuchVisualElementException(
                String.format("Visual element with text '%s' not found %s anchor '%s'", targetText, direction.getDirection(), anchorText));
    }

    public VisualElement findRelativeByText(String targetText, SpatialDirection direction, VisualElement anchor) {
        verifyOcrEnabled();
        LOGGER.info("Locating visual element '{}' {} anchor element", targetText, direction.getDirection());
        List<VisualElement> candidates = findAllByText(targetText);

        VisualElement bestCandidate = filterAndSelectClosestRelative(anchor, candidates, direction);
        if (bestCandidate != null) {
            return bestCandidate;
        }
        throw new NoSuchVisualElementException(
                String.format("Visual element with text '%s' not found %s anchor element", targetText, direction.getDirection()));
    }

    public VisualElement findRelativeByImage(List<String> targetImagePaths, SpatialDirection direction, String anchorText) {
        verifyOcrEnabled();
        LOGGER.info("Locating visual element matching images {} {} anchor text '{}'", targetImagePaths, direction.getDirection(), anchorText);
        VisualElement anchor = findByText(anchorText);
        List<VisualElement> candidates = findAllByImage(targetImagePaths);

        VisualElement bestCandidate = filterAndSelectClosestRelative(anchor, candidates, direction);
        if (bestCandidate != null) {
            return bestCandidate;
        }
        throw new NoSuchVisualElementException(
                String.format("Visual element matching images %s not found %s anchor '%s'", targetImagePaths, direction.getDirection(), anchorText));
    }

    public VisualElement findRelativeByImage(List<String> targetImagePaths, SpatialDirection direction, VisualElement anchor) {
        verifyOcrEnabled();
        LOGGER.info("Locating visual element matching images {} {} anchor element", targetImagePaths, direction.getDirection());
        List<VisualElement> candidates = findAllByImage(targetImagePaths);

        VisualElement bestCandidate = filterAndSelectClosestRelative(anchor, candidates, direction);
        if (bestCandidate != null) {
            return bestCandidate;
        }
        throw new NoSuchVisualElementException(
                String.format("Visual element matching images %s not found %s anchor element", targetImagePaths, direction.getDirection()));
    }

    private VisualElement filterAndSelectClosestRelative(VisualElement anchor, List<VisualElement> candidates, SpatialDirection direction) {
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
     * Sleeps for {@code retryDelayMs} between find attempts. Returns {@code false} and restores the
     * thread's interrupt flag if interrupted, signalling the caller to stop retrying.
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

    private void verifyOcrEnabled() {
        if (!Runner.isOcrEnabled()) {
            throw new VisualSubsystemDisabledException(
                "\n====================================================================================================\n" +
                " [teswiz] Visual OCR & Image Recognition Subsystem is Disabled!\n" +
                "----------------------------------------------------------------------------------------------------\n" +
                " You invoked a visual locator method (e.g. driver.findByText() or driver.findByImage()), but \n" +
                " IS_OCR_ENABLED=false in your execution configuration (teswiz_config.properties).\n\n" +
                " To enable OCR & Image Recognition capability in your project:\n" +
                " 1. Set 'IS_OCR_ENABLED=true' in your teswiz_config.properties file or system property.\n" +
                " 2. Ensure native visual dependencies (opencv, tess4j) are downloaded via ./gradlew downloadDependencies.\n" +
                "====================================================================================================\n"
            );
        }
    }
}
