package com.znsio.teswiz.screen.transportnsw;

import com.znsio.teswiz.runner.VisualElement;
import com.znsio.teswiz.screen.ScreenRegistry;

public abstract class TransportNswOcrScreen {

    public static TransportNswOcrScreen get() {
        return ScreenRegistry.getScreen(TransportNswOcrScreen.class);
    }

    public abstract TransportNswOcrScreen navigateTo(String url);

    public abstract TransportNswOcrScreen scrollToExploreRouteMap();

    public abstract VisualElement clickStationGreenDotOnMap(String greenDotImageTemplatePath);

    public abstract VisualElement clickCalloutOptionByText(String text);

    public abstract VisualElement clickStationNameOnMapByText(String stationName);

    public abstract boolean isStationPageDisplayedFor(String stationName);
}
