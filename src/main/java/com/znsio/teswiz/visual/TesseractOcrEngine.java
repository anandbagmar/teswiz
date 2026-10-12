package com.znsio.teswiz.visual;

import java.util.List;

import com.znsio.teswiz.entities.VisualRegion;
import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.VisualElement;

/**
 * Default {@link OcrEngine} backed by Tesseract via the static {@link OcrService}. This is a thin
 * delegating adapter - all OCR behaviour continues to live in {@link OcrService}.
 */
public class TesseractOcrEngine implements OcrEngine {

    @Override
    public VisualElement findTextMatch(byte[] screenshotBytes, String searchText, VisualRegion region, Driver driverFacade) {
        return OcrService.findTextMatch(screenshotBytes, searchText, region, driverFacade);
    }

    @Override
    public List<VisualElement> findAllTextMatches(byte[] screenshotBytes, String searchText, VisualRegion region, Driver driverFacade) {
        return OcrService.findAllTextMatches(screenshotBytes, searchText, region, driverFacade);
    }
}
