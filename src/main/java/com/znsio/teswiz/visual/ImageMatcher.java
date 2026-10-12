package com.znsio.teswiz.visual;

import java.util.List;

import com.znsio.teswiz.entities.VisualRegion;
import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.VisualElement;

/**
 * Abstraction over the image-template matching backend. The default implementation
 * ({@link OpenCvImageMatcher}) delegates to the OpenCV-backed {@link ImageRecognitionService};
 * introducing this seam lets the visual finder be unit-tested with a fake matcher and lets the
 * matching backend be swapped without touching the finder.
 */
public interface ImageMatcher {

    /**
     * Finds the first visual element matching any of the {@code templatePaths} at or above
     * {@code confidenceThreshold} within an optional {@code region} of the given screenshot.
     * Returns {@code null} when no match is found.
     */
    VisualElement findTemplateMatch(byte[] screenshotBytes, List<String> templatePaths, double confidenceThreshold,
                                    VisualRegion region, Driver driverFacade);

    /**
     * Finds all visual elements matching any of the {@code templatePaths} at or above
     * {@code confidenceThreshold} within an optional {@code region} of the given screenshot.
     * Returns an empty list when none match.
     */
    List<VisualElement> findAllTemplateMatches(byte[] screenshotBytes, List<String> templatePaths, double confidenceThreshold,
                                               VisualRegion region, Driver driverFacade);
}
