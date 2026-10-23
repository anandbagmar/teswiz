package com.znsio.teswiz.screen.transportnsw;

import com.znsio.teswiz.entities.VisionStrategy;
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

    /**
     * Locates and clicks an element within the "Route Map" panel region using the given strategy
     * ({@code OCR_THEN_LLM} or {@code LLM}). Scoping the locate to the panel keeps the vision model
     * from returning a coordinate elsewhere on the page (e.g. in the body text), which is the common
     * failure mode when describing a control on a dense page.
     */
    public abstract VisualElement clickElementInRouteMapPanel(String elementName, VisionStrategy strategy,
            String description);

    /**
     * Verifies the "Select route" dropdown's options list is open/displayed - i.e. the preceding
     * click actually landed on and activated the dropdown. Returns {@code true} when the options
     * list is present in the DOM, so a mis-located click (which would not open it) fails the test.
     */
    public abstract boolean isRouteSelectorOptionsListDisplayed();

    public abstract VisualElement clickCalloutOptionByText(String text);

    public abstract VisualElement clickStationNameOnMapByText(String stationName);

    public abstract boolean isStationPageDisplayedFor(String stationName);
}
