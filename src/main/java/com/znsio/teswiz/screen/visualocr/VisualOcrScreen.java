package com.znsio.teswiz.screen.visualocr;

import java.util.List;

import com.znsio.teswiz.entities.Direction;
import com.znsio.teswiz.entities.SpatialDirection;
import com.znsio.teswiz.entities.VisualRegion;
import com.znsio.teswiz.runner.VisualElement;
import com.znsio.teswiz.screen.ScreenRegistry;

public abstract class VisualOcrScreen {

    public static VisualOcrScreen get() {
        return ScreenRegistry.getScreen(VisualOcrScreen.class);
    }

    public abstract VisualElement findVisualElementByText(String text);
    public abstract VisualElement findVisualElementByImage(List<String> imagePaths);
    public abstract VisualElement findVisualElementByTextOrImage(String text, List<String> imagePaths);
    public abstract VisualElement findVisualElementByImageOrText(List<String> imagePaths, String text);

    public abstract List<VisualElement> findAllVisualElementsByText(String text);
    public abstract List<VisualElement> findAllVisualElementsByImage(List<String> imagePaths);
    public abstract List<VisualElement> findAllVisualElementsByTextOrImage(String text, List<String> imagePaths);
    public abstract List<VisualElement> findAllVisualElementsByImageOrText(List<String> imagePaths, String text);

    public abstract VisualElement findVisualElementRelativeByText(String targetText, SpatialDirection direction, String anchorText);
    public abstract VisualElement findVisualElementRelativeByImage(List<String> imagePaths, SpatialDirection direction, String anchorText);
    public abstract VisualElement findVisualElementByTextInRegion(String text, VisualRegion region);
    public abstract VisualElement findVisualElementByImageInRegion(List<String> imagePaths, VisualRegion region);

    // Screen action contracts adhering to Feature -> Step -> BL -> Screen pattern
    public abstract VisualOcrScreen clickVisualElementByText(String elementName, String ocrText);
    public abstract VisualOcrScreen clickVisualElementByImage(String elementName, List<String> imagePaths);
    public abstract VisualOcrScreen clickVisualElementByTextOrImage(String elementName, String ocrText, List<String> imagePaths);
    public abstract VisualOcrScreen clickVisualElementByImageOrText(String elementName, List<String> imagePaths, String ocrText);

    public abstract VisualOcrScreen enterTextIntoVisualElementByText(String elementName, String textToEnter, String ocrText);
    public abstract VisualOcrScreen inspectVisualElementByText(String elementName, String ocrText);
    public abstract VisualOcrScreen inspectVisualElementByImage(String elementName, List<String> imagePaths);

    public abstract VisualOcrScreen doubleClickVisualElementByText(String elementName, String ocrText);
    public abstract VisualOcrScreen hoverVisualElementByText(String elementName, String ocrText);
    public abstract VisualOcrScreen longPressVisualElementByText(String elementName, String ocrText);
    public abstract VisualOcrScreen swipeOnVisualElementByText(String elementName, Direction direction, String ocrText);

    public abstract VisualOcrScreen clickVisualElementAtIndexByText(String elementName, int index, String ocrText);
    public abstract VisualOcrScreen clickVisualElementByPositionByText(String elementName, String positionText, String ocrText);
    public abstract VisualOcrScreen clickVisualElementByPositionByImage(String elementName, String positionText, List<String> imagePaths);
    public abstract VisualOcrScreen clickVisualElementRelativeByText(String elementName, String targetText, SpatialDirection direction, String anchorText);

    public abstract boolean tryClickVisualElementByText(String elementName, String ocrText);
    public abstract boolean tryClickVisualElementByImage(String elementName, List<String> imagePaths);
    public abstract boolean tryClickVisualElementByTextOrImage(String elementName, String ocrText, List<String> imagePaths);
    public abstract boolean tryClickVisualElementByImageOrText(String elementName, List<String> imagePaths, String ocrText);
    public abstract boolean tryClickVisualElementRelativeByText(String elementName, String targetText, SpatialDirection direction, String anchorText);
}
