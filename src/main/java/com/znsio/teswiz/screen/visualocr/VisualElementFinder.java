package com.znsio.teswiz.screen.visualocr;

import java.util.List;

import com.znsio.teswiz.entities.SpatialDirection;
import com.znsio.teswiz.entities.VisualRegion;
import com.znsio.teswiz.runner.VisualElement;

/**
 * Locating role of a Visual OCR screen: find a single element or all matching elements by OCR text, image
 * template, fallbacks, spatial relation to an anchor, or within a region. A {@code null} (or empty list) result
 * means nothing matched on the current screen capture. Segregated so a consumer that only needs to locate
 * elements need not depend on the action/inspection roles.
 */
public interface VisualElementFinder {

    VisualElement findVisualElementByText(String text);
    VisualElement findVisualElementByImage(List<String> imagePaths);
    VisualElement findVisualElementByTextOrImage(String text, List<String> imagePaths);
    VisualElement findVisualElementByImageOrText(List<String> imagePaths, String text);

    List<VisualElement> findAllVisualElementsByText(String text);
    List<VisualElement> findAllVisualElementsByImage(List<String> imagePaths);
    List<VisualElement> findAllVisualElementsByTextOrImage(String text, List<String> imagePaths);
    List<VisualElement> findAllVisualElementsByImageOrText(List<String> imagePaths, String text);

    VisualElement findVisualElementRelativeByText(String targetText, SpatialDirection direction, String anchorText);
    VisualElement findVisualElementRelativeByImage(List<String> imagePaths, SpatialDirection direction, String anchorText);
    VisualElement findVisualElementByTextInRegion(String text, VisualRegion region);
    VisualElement findVisualElementByImageInRegion(List<String> imagePaths, VisualRegion region);
}
