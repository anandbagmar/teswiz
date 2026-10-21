package com.znsio.teswiz.runner;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.entities.SpatialDirection;
import com.znsio.teswiz.entities.VisionStrategy;
import com.znsio.teswiz.entities.VisualRegion;
import com.znsio.teswiz.exceptions.NoSuchVisualElementException;
import com.znsio.teswiz.visual.OcrReadHint;
import com.znsio.teswiz.visual.OcrService;

/**
 * Strategy-scoped visual locator surface. A {@link VisionFinder} is bound to a single {@link VisionStrategy} (OCR, LLM,
 * or OCR-then-LLM) at creation time; every finder method on it routes to the appropriate engine on the shared
 * {@link VisualFinder} according to that strategy.
 *
 * <p>
 * Obtain one via {@code driver.vision(strategy)} or {@code visual.vision(strategy)}. The same method names are exposed
 * regardless of strategy - the chosen strategy decides which engine runs. For the LLM strategy, {@code text} is a
 * natural-language description and image paths are reference/example images.
 */
public class VisionFinder {
    private static final Logger LOGGER = LogManager.getLogger(VisionFinder.class.getName());

    private final VisionStrategy strategy;
    private final VisualFinder visualFinder;

    VisionFinder(VisionStrategy strategy, VisualFinder visualFinder) {
        this.strategy = strategy;
        this.visualFinder = visualFinder;
    }

    public VisionStrategy getStrategy() {
        return this.strategy;
    }

    // ------------------------------------------------------------------------
    // findByText
    // ------------------------------------------------------------------------

    public VisualElement findByText(String text) {
        return findByText(text, null);
    }

    public VisualElement findByText(String text, VisualRegion region) {
        switch (this.strategy) {
            case LLM:
                return this.visualFinder.findByDescription(text, (List<String>) null, region);
            case OCR_THEN_LLM:
                return this.visualFinder.findByTextOrImageOrDescription(text, Collections.emptyList(), text, region);
            case OCR:
            default:
                return region != null ? this.visualFinder.findByText(text, region) : this.visualFinder.findByText(text);
        }
    }

    // ------------------------------------------------------------------------
    // readText / readValue / readNumber (read an unknown value from a region)
    //
    // These READ the value in a region (what text/number is there) rather than LOCATING an element
    // matching a supplied string. Routing mirrors the find* methods:
    // OCR -> OcrService read (deterministic default); the description is ignored.
    // LLM -> LlmVisionService.readTextByDescription (needs a description).
    // OCR_THEN_LLM -> OCR read first, falling back to the LLM read when the OCR result is blank
    // (readText) or unparseable/empty (readNumber).
    // The description-free overloads are intended for the OCR strategy; under LLM / OCR_THEN_LLM a
    // description is required for the LLM arm, so use the readValue(region, description, ...) overloads.
    // ------------------------------------------------------------------------

    /**
     * Reads the text in {@code region} using the {@link OcrReadHint#defaults() default} OCR hint. Shorthand for
     * {@link #readValue(VisualRegion, String, OcrReadHint) readValue(region, null, defaults)}; see that method for the
     * strategy behaviour (the LLM arm requires a description, so prefer {@code readValue} under LLM / OCR_THEN_LLM).
     */
    public String readText(VisualRegion region) {
        return readValue(region, null, OcrReadHint.defaults());
    }

    /**
     * Reads the text in {@code region} using the given OCR {@code hint}. Shorthand for
     * {@link #readValue(VisualRegion, String, OcrReadHint) readValue(region, null, hint)}.
     */
    public String readText(VisualRegion region, OcrReadHint hint) {
        return readValue(region, null, hint);
    }

    /**
     * Reads the value in {@code region}, routed by strategy, using the default OCR hint. The {@code description} is
     * used by the LLM / OCR_THEN_LLM arms to tell the model what to read and is ignored under pure OCR.
     */
    public String readValue(VisualRegion region, String description) {
        return readValue(region, description, OcrReadHint.defaults());
    }

    /**
     * Reads the value in {@code region}, routed by strategy.
     *
     * @param region      the region to read; {@code null} reads the whole screen
     * @param description what to read, used by the LLM and OCR_THEN_LLM arms (ignored under pure OCR); may be
     *                    {@code null}/blank only when the strategy is OCR
     * @param hint        the OCR read tuning (used by the OCR and OCR_THEN_LLM arms)
     * @return the read text, trimmed; an empty string when nothing is read (never {@code null})
     */
    public String readValue(VisualRegion region, String description, OcrReadHint hint) {
        switch (this.strategy) {
            case LLM:
                return this.visualFinder.readTextViaLlm(description, region);
            case OCR_THEN_LLM:
                String ocrRead = this.visualFinder.readTextViaOcr(region, hint);
                if (!ocrRead.isBlank()) {
                    return ocrRead;
                }
                LOGGER.info("OCR read was blank under OCR_THEN_LLM; falling back to LLM read (description='{}')",
                        description);
                return this.visualFinder.readTextViaLlm(description, region);
            case OCR:
            default:
                return this.visualFinder.readTextViaOcr(region, hint);
        }
    }

    /**
     * Reads a number in {@code region}, routed by strategy, with no description (intended for the OCR strategy).
     * Shorthand for {@link #readNumber(VisualRegion, String) readNumber(region, null)}.
     */
    public Optional<BigDecimal> readNumber(VisualRegion region) {
        return readNumber(region, null);
    }

    /**
     * Reads a number in {@code region}, routed by strategy.
     *
     * <p>
     * OCR parses the OCR read directly. The LLM arm reads the value by {@code description} and then numeric-parses it.
     * OCR_THEN_LLM tries the OCR number first and falls back to the LLM read+parse only when the OCR result is
     * empty/unparseable.
     *
     * @param region      the region to read; {@code null} reads the whole screen
     * @param description what to read, used by the LLM and OCR_THEN_LLM arms (ignored under pure OCR)
     * @return the parsed value, or {@link Optional#empty()} when nothing numeric is read (never throws)
     */
    public Optional<BigDecimal> readNumber(VisualRegion region, String description) {
        switch (this.strategy) {
            case LLM:
                return OcrService.normalizeNumber(this.visualFinder.readTextViaLlm(description, region));
            case OCR_THEN_LLM:
                Optional<BigDecimal> ocrNumber = this.visualFinder.readNumberViaOcr(region);
                if (ocrNumber.isPresent()) {
                    return ocrNumber;
                }
                LOGGER.info(
                        "OCR number read was empty/unparseable under OCR_THEN_LLM; falling back to LLM read (description='{}')",
                        description);
                return OcrService.normalizeNumber(this.visualFinder.readTextViaLlm(description, region));
            case OCR:
            default:
                return this.visualFinder.readNumberViaOcr(region);
        }
    }

    // ------------------------------------------------------------------------
    // findByImage
    // ------------------------------------------------------------------------

    public VisualElement findByImage(List<String> imagePaths) {
        return findByImageInternal(imagePaths, null, null);
    }

    public VisualElement findByImage(List<String> imagePaths, double confidenceThreshold) {
        return findByImageInternal(imagePaths, confidenceThreshold, null);
    }

    public VisualElement findByImage(List<String> imagePaths, VisualRegion region) {
        return findByImageInternal(imagePaths, null, region);
    }

    public VisualElement findByImage(List<String> imagePaths, double confidenceThreshold, VisualRegion region) {
        return findByImageInternal(imagePaths, confidenceThreshold, region);
    }

    private VisualElement findByImageInternal(List<String> imagePaths, Double confidenceThreshold,
            VisualRegion region) {
        switch (this.strategy) {
            case LLM:
                return this.visualFinder.findByDescription(null, imagePaths, region);
            case OCR_THEN_LLM:
                try {
                    return ocrFindByImage(imagePaths, confidenceThreshold, region);
                } catch (NoSuchVisualElementException ocrMiss) {
                    LOGGER.info(
                            "OCR image match missed under OCR_THEN_LLM; falling back to LLM with reference images {}",
                            imagePaths);
                    return this.visualFinder.findByDescription(null, imagePaths, region);
                }
            case OCR:
            default:
                return ocrFindByImage(imagePaths, confidenceThreshold, region);
        }
    }

    private VisualElement ocrFindByImage(List<String> imagePaths, Double confidenceThreshold, VisualRegion region) {
        if (confidenceThreshold != null && region != null) {
            return this.visualFinder.findByImage(imagePaths, confidenceThreshold, region);
        }
        if (confidenceThreshold != null) {
            return this.visualFinder.findByImage(imagePaths, confidenceThreshold);
        }
        if (region != null) {
            return this.visualFinder.findByImage(imagePaths, region);
        }
        return this.visualFinder.findByImage(imagePaths);
    }

    // ------------------------------------------------------------------------
    // findByTextOrImage
    // ------------------------------------------------------------------------

    public VisualElement findByTextOrImage(String text, List<String> imagePaths) {
        return findByTextOrImage(text, imagePaths, null);
    }

    public VisualElement findByTextOrImage(String text, List<String> imagePaths, VisualRegion region) {
        switch (this.strategy) {
            case LLM:
                return this.visualFinder.findByDescription(text, imagePaths, region);
            case OCR_THEN_LLM:
                return this.visualFinder.findByTextOrImageOrDescription(text, imagePaths, text, region);
            case OCR:
            default:
                return region != null ? this.visualFinder.findByTextOrImage(text, imagePaths, region)
                        : this.visualFinder.findByTextOrImage(text, imagePaths);
        }
    }

    // ------------------------------------------------------------------------
    // findByImageOrText (OCR-only; others unsupported)
    // ------------------------------------------------------------------------

    public VisualElement findByImageOrText(List<String> imagePaths, String text) {
        if (this.strategy != VisionStrategy.OCR) {
            throw unsupportedUnderStrategy("findByImageOrText");
        }
        return this.visualFinder.findByImageOrText(imagePaths, text);
    }

    // ------------------------------------------------------------------------
    // findAllByText
    // ------------------------------------------------------------------------

    public List<VisualElement> findAllByText(String text) {
        return findAllByText(text, null);
    }

    public List<VisualElement> findAllByText(String text, VisualRegion region) {
        switch (this.strategy) {
            case LLM:
                return singleMatchViaLlm(text, null, region);
            case OCR_THEN_LLM:
                List<VisualElement> ocrMatches = ocrFindAllByText(text, region);
                if (!ocrMatches.isEmpty()) {
                    return ocrMatches;
                }
                return singleMatchViaLlm(text, null, region);
            case OCR:
            default:
                return ocrFindAllByText(text, region);
        }
    }

    private List<VisualElement> ocrFindAllByText(String text, VisualRegion region) {
        return region != null ? this.visualFinder.findAllByText(text, region) : this.visualFinder.findAllByText(text);
    }

    // ------------------------------------------------------------------------
    // findAllByImage
    // ------------------------------------------------------------------------

    public List<VisualElement> findAllByImage(List<String> imagePaths) {
        return findAllByImageInternal(imagePaths, null, null);
    }

    public List<VisualElement> findAllByImage(List<String> imagePaths, double confidenceThreshold) {
        return findAllByImageInternal(imagePaths, confidenceThreshold, null);
    }

    public List<VisualElement> findAllByImage(List<String> imagePaths, VisualRegion region) {
        return findAllByImageInternal(imagePaths, null, region);
    }

    public List<VisualElement> findAllByImage(List<String> imagePaths, double confidenceThreshold,
            VisualRegion region) {
        return findAllByImageInternal(imagePaths, confidenceThreshold, region);
    }

    private List<VisualElement> findAllByImageInternal(List<String> imagePaths, Double confidenceThreshold,
            VisualRegion region) {
        switch (this.strategy) {
            case LLM:
                return singleMatchViaLlm(null, imagePaths, region);
            case OCR_THEN_LLM:
                List<VisualElement> ocrMatches = ocrFindAllByImage(imagePaths, confidenceThreshold, region);
                if (!ocrMatches.isEmpty()) {
                    return ocrMatches;
                }
                return singleMatchViaLlm(null, imagePaths, region);
            case OCR:
            default:
                return ocrFindAllByImage(imagePaths, confidenceThreshold, region);
        }
    }

    private List<VisualElement> ocrFindAllByImage(List<String> imagePaths, Double confidenceThreshold,
            VisualRegion region) {
        if (confidenceThreshold != null && region != null) {
            return this.visualFinder.findAllByImage(imagePaths, confidenceThreshold, region);
        }
        if (confidenceThreshold != null) {
            return this.visualFinder.findAllByImage(imagePaths, confidenceThreshold);
        }
        if (region != null) {
            return this.visualFinder.findAllByImage(imagePaths, region);
        }
        return this.visualFinder.findAllByImage(imagePaths);
    }

    // ------------------------------------------------------------------------
    // findAllByTextOrImage / findAllByImageOrText (OCR-only; others unsupported)
    // ------------------------------------------------------------------------

    public List<VisualElement> findAllByTextOrImage(String text, List<String> imagePaths) {
        if (this.strategy != VisionStrategy.OCR) {
            throw unsupportedUnderStrategy("findAllByTextOrImage");
        }
        return this.visualFinder.findAllByTextOrImage(text, imagePaths);
    }

    public List<VisualElement> findAllByImageOrText(List<String> imagePaths, String text) {
        if (this.strategy != VisionStrategy.OCR) {
            throw unsupportedUnderStrategy("findAllByImageOrText");
        }
        return this.visualFinder.findAllByImageOrText(imagePaths, text);
    }

    // ------------------------------------------------------------------------
    // findRelativeByText
    // ------------------------------------------------------------------------

    public VisualElement findRelativeByText(String targetText, SpatialDirection direction, String anchorText) {
        if (this.strategy == VisionStrategy.LLM) {
            throw unsupportedRelativeUnderLlm();
        }
        // OCR and OCR_THEN_LLM both use OCR relative selection; no LLM fallback for relative finds.
        return this.visualFinder.findRelativeByText(targetText, direction, anchorText);
    }

    public VisualElement findRelativeByText(String targetText, SpatialDirection direction, VisualElement anchor) {
        if (this.strategy == VisionStrategy.LLM) {
            throw unsupportedRelativeUnderLlm();
        }
        return this.visualFinder.findRelativeByText(targetText, direction, anchor);
    }

    // ------------------------------------------------------------------------
    // findRelativeByImage
    // ------------------------------------------------------------------------

    public VisualElement findRelativeByImage(List<String> targetImagePaths, SpatialDirection direction,
            String anchorText) {
        if (this.strategy == VisionStrategy.LLM) {
            throw unsupportedRelativeUnderLlm();
        }
        return this.visualFinder.findRelativeByImage(targetImagePaths, direction, anchorText);
    }

    public VisualElement findRelativeByImage(List<String> targetImagePaths, SpatialDirection direction,
            VisualElement anchor) {
        if (this.strategy == VisionStrategy.LLM) {
            throw unsupportedRelativeUnderLlm();
        }
        return this.visualFinder.findRelativeByImage(targetImagePaths, direction, anchor);
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    /**
     * LLM strategies have no "find all" engine; a single located element is returned as a one-element list. A miss
     * ({@link NoSuchVisualElementException}) yields an empty list.
     */
    private List<VisualElement> singleMatchViaLlm(String description, List<String> referenceImagePaths,
            VisualRegion region) {
        try {
            VisualElement match = this.visualFinder.findByDescription(description, referenceImagePaths, region);
            return match != null ? Collections.singletonList(match) : Collections.emptyList();
        } catch (NoSuchVisualElementException e) {
            return Collections.emptyList();
        }
    }

    private UnsupportedOperationException unsupportedUnderStrategy(String methodName) {
        return new UnsupportedOperationException(
                methodName + " is only supported under VisionStrategy.OCR (current strategy: " + this.strategy + ").");
    }

    private UnsupportedOperationException unsupportedRelativeUnderLlm() {
        return new UnsupportedOperationException(
                "Relative finds (findRelativeByText/findRelativeByImage) are not supported under VisionStrategy.LLM. "
                        + "Use VisionStrategy.OCR or VisionStrategy.OCR_THEN_LLM for spatial/relative lookups.");
    }
}
