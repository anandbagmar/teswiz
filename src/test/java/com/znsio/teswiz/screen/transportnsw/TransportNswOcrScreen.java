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

    public abstract VisualElement clickCalloutOptionByText(String text);

    public abstract VisualElement clickStationNameOnMapByText(String stationName);

    public abstract boolean isStationPageDisplayedFor(String stationName);
}
