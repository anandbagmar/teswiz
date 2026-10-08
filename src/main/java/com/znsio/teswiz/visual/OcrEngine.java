package com.znsio.teswiz.visual;

import java.util.List;

import com.znsio.teswiz.entities.VisualRegion;
import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.VisualElement;

/**
 * Abstraction over the OCR text-recognition backend. The default implementation
 * ({@link TesseractOcrEngine}) delegates to the Tesseract-backed {@link OcrService}; introducing
 * this seam lets the visual finder be unit-tested with a fake engine and lets the OCR backend be
 * swapped without touching the finder.
 */
public interface OcrEngine {

    /**
     * Finds the first visual element whose OCR text matches {@code searchText} within an optional
     * {@code region} of the given screenshot. Returns {@code null} when no match is found.
     */
    VisualElement findTextMatch(byte[] screenshotBytes, String searchText, VisualRegion region, Driver driverFacade);

    /**
     * Finds all visual elements whose OCR text matches {@code searchText} within an optional
     * {@code region} of the given screenshot. Returns an empty list when none match.
     */
    List<VisualElement> findAllTextMatches(byte[] screenshotBytes, String searchText, VisualRegion region, Driver driverFacade);
}
