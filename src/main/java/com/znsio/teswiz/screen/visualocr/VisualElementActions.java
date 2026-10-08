package com.znsio.teswiz.screen.visualocr;

import java.util.List;

import com.znsio.teswiz.entities.Direction;
import com.znsio.teswiz.entities.SpatialDirection;

/**
 * Acting role of a Visual OCR screen: perform a gesture on a located element. Strict actions fail if the element
 * is not found; {@code tryClick*} actions return whether the element was found and acted on. Segregated from the
 * finding and inspection roles so callers depend only on what they use.
 */
public interface VisualElementActions {

    VisualOcrScreen clickVisualElementByText(String elementName, String ocrText);
    VisualOcrScreen clickVisualElementByImage(String elementName, List<String> imagePaths);
    VisualOcrScreen clickVisualElementByTextOrImage(String elementName, String ocrText, List<String> imagePaths);
    VisualOcrScreen clickVisualElementByImageOrText(String elementName, List<String> imagePaths, String ocrText);

    VisualOcrScreen enterTextIntoVisualElementByText(String elementName, String textToEnter, String ocrText);

    VisualOcrScreen doubleClickVisualElementByText(String elementName, String ocrText);
    VisualOcrScreen hoverVisualElementByText(String elementName, String ocrText);
    VisualOcrScreen longPressVisualElementByText(String elementName, String ocrText);
    VisualOcrScreen swipeOnVisualElementByText(String elementName, Direction direction, String ocrText);

    VisualOcrScreen clickVisualElementAtIndexByText(String elementName, int index, String ocrText);
    VisualOcrScreen clickVisualElementByPositionByText(String elementName, String positionText, String ocrText);
    VisualOcrScreen clickVisualElementByPositionByImage(String elementName, String positionText, List<String> imagePaths);
    VisualOcrScreen clickVisualElementRelativeByText(String elementName, String targetText, SpatialDirection direction, String anchorText);

    boolean tryClickVisualElementByText(String elementName, String ocrText);
    boolean tryClickVisualElementByImage(String elementName, List<String> imagePaths);
    boolean tryClickVisualElementByTextOrImage(String elementName, String ocrText, List<String> imagePaths);
    boolean tryClickVisualElementByImageOrText(String elementName, List<String> imagePaths, String ocrText);
    boolean tryClickVisualElementRelativeByText(String elementName, String targetText, SpatialDirection direction, String anchorText);
}
