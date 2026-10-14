package com.znsio.teswiz.visual;

import java.util.List;

import com.znsio.teswiz.entities.VisualRegion;
import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.VisualElement;

/**
 * Abstraction over an LLM (vision model) backend used as a last-resort element locator when both
 * OCR ({@link OcrEngine}) and image-template matching ({@link ImageMatcher}) fail. The default
 * implementation ({@link LlmVisionService}) sends the screenshot plus a natural-language
 * description (and optional reference/example images of the target) to a vision-capable chat
 * completions endpoint (OpenAI-compatible wire format) via the framework's configured API engine,
 * and parses the returned bounding box.
 *
 * <p>Introducing this seam keeps the finder engine-agnostic and lets the backend be swapped or
 * faked in tests, mirroring the {@link OcrEngine} / {@link ImageMatcher} seams.
 */
public interface LlmVisionEngine {

    /**
     * Locates the element described by {@code description} within an optional {@code region} of the
     * given screenshot. Returns {@code null} when the model cannot locate it (or when the engine is
     * unavailable / disabled).
     *
     * @param screenshotBytes the full-screen (or region source) screenshot bytes
     * @param description      natural-language description of the target element
     * @param region          optional region to constrain the search; {@code null} for the whole screen
     * @param driverFacade     the owning driver facade, used for coordinate scaling and to act on the element
     * @return a located {@link VisualElement} in logical viewport coordinates, or {@code null}
     */
    VisualElement findByDescription(byte[] screenshotBytes, String description, VisualRegion region, Driver driverFacade);

    /**
     * Locates an element on the given screenshot using a natural-language {@code description}
     * and/or one or more reference/example images of the target element. The reference images are
     * supplied as visual exemplars ("find the element that looks like these"); the returned
     * bounding box is always in the coordinate space of the <em>screenshot</em>, never a reference
     * image. At least one of {@code description} or {@code referenceImagePaths} must be provided.
     *
     * @param screenshotBytes     the full-screen (or region source) screenshot bytes
     * @param description         natural-language description of the target element; may be {@code null}/blank
     *                            when {@code referenceImagePaths} are supplied
     * @param referenceImagePaths file paths to reference/example images of the target; may be {@code null}/empty
     * @param region              optional region to constrain the search; {@code null} for the whole screen
     * @param driverFacade        the owning driver facade, used for coordinate scaling and to act on the element
     * @return a located {@link VisualElement} in logical viewport coordinates, or {@code null}
     */
    VisualElement findByDescription(byte[] screenshotBytes, String description, List<String> referenceImagePaths,
                                    VisualRegion region, Driver driverFacade);
}
