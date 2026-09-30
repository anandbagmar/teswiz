package com.znsio.teswiz.screen.visualocr;

import java.util.List;

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

    public abstract VisualElement findVisualElementRelativeByText(String targetText, com.znsio.teswiz.entities.SpatialDirection direction, String anchorText);

    public abstract VisualElement findVisualElementRelativeByImage(List<String> imagePaths, com.znsio.teswiz.entities.SpatialDirection direction, String anchorText);
}

