package com.znsio.teswiz.screen.transportnsw;

import com.znsio.teswiz.runner.VisualElement;
import com.znsio.teswiz.screen.ScreenRegistry;

public abstract class TransportNswOcrScreen {

    public static TransportNswOcrScreen get() {
        return ScreenRegistry.getScreen(TransportNswOcrScreen.class);
    }

    public abstract TransportNswOcrScreen navigateTo(String url);

    public abstract TransportNswOcrScreen scrollToExploreRouteMap();

    public abstract VisualElement clickElementByImage(String elementName, String imagePath);

    public abstract VisualElement clickElementByOcrText(String elementName, String ocrText);

    /**
     * Locates and clicks an element using the OCR-then-LLM fallback strategy: OCR/image first, and
     * only if that misses, a vision LLM locates it from the natural-language {@code description}.
     */
    public abstract VisualElement clickElementByOcrThenLlm(String elementName, String description);

    /**
     * Locates and clicks an element using the LLM vision strategy only: a vision LLM locates it from
     * the natural-language {@code description}.
     */
    public abstract VisualElement clickElementByLlm(String elementName, String description);

    public abstract VisualElement clickCalloutOptionByText(String text);

    public abstract VisualElement clickStationNameOnMapByText(String stationName);

    public abstract boolean isStationPageDisplayedFor(String stationName);
}
