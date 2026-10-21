package com.znsio.teswiz.visual;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.znsio.teswiz.entities.VisualRegion;
import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.VisualElement;

/**
 * Abstraction over the OCR text-recognition backend. The default implementation ({@link TesseractOcrEngine}) delegates
 * to the Tesseract-backed {@link OcrService}; introducing this seam lets the visual finder be unit-tested with a fake
 * engine and lets the OCR backend be swapped without touching the finder.
 */
public interface OcrEngine {

    /**
     * Finds the first visual element whose OCR text matches {@code searchText} within an optional {@code region} of the
     * given screenshot. Returns {@code null} when no match is found.
     */
    VisualElement findTextMatch(byte[] screenshotBytes, String searchText, VisualRegion region, Driver driverFacade);

    /**
     * Finds all visual elements whose OCR text matches {@code searchText} within an optional {@code region} of the
     * given screenshot. Returns an empty list when none match.
     */
    List<VisualElement> findAllTextMatches(byte[] screenshotBytes, String searchText, VisualRegion region,
            Driver driverFacade);

    /**
     * Reads (recognises) and returns the text within an optional {@code region} of the screenshot, tuned by
     * {@code hint}. Unlike {@link #findTextMatch}, this returns whatever is recognised rather than matching a supplied
     * string. Returns an empty string when nothing is read or the engine is unavailable (never {@code null}).
     */
    String readText(byte[] screenshotBytes, VisualRegion region, Driver driverFacade, OcrReadHint hint);

    /**
     * Reads a numeric value within an optional {@code region} of the screenshot and normalises it. Returns
     * {@link Optional#empty()} when the read is blank or not numeric; never throws for an unreadable region.
     */
    Optional<BigDecimal> readNumber(byte[] screenshotBytes, VisualRegion region, Driver driverFacade);
}
