package com.znsio.teswiz.visual;

import java.util.List;

import com.znsio.teswiz.entities.VisualRegion;
import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.VisualElement;

/**
 * Default {@link ImageMatcher} backed by OpenCV via the static {@link ImageRecognitionService}.
 * This is a thin delegating adapter - all template-matching behaviour continues to live in
 * {@link ImageRecognitionService}.
 */
public class OpenCvImageMatcher implements ImageMatcher {

    @Override
    public VisualElement findTemplateMatch(byte[] screenshotBytes, List<String> templatePaths, double confidenceThreshold,
                                           VisualRegion region, Driver driverFacade) {
        return ImageRecognitionService.findTemplateMatch(screenshotBytes, templatePaths, confidenceThreshold, region, driverFacade);
    }

    @Override
    public List<VisualElement> findAllTemplateMatches(byte[] screenshotBytes, List<String> templatePaths, double confidenceThreshold,
                                                      VisualRegion region, Driver driverFacade) {
        return ImageRecognitionService.findAllTemplateMatches(screenshotBytes, templatePaths, confidenceThreshold, region, driverFacade);
    }
}
