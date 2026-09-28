package com.znsio.teswiz.screen.covidtracking;

import com.znsio.teswiz.runner.VisualElement;
import com.znsio.teswiz.screen.ScreenRegistry;

public abstract class CovidTrackingScreen {

    public static CovidTrackingScreen get() {
        return ScreenRegistry.getScreen(CovidTrackingScreen.class);
    }

    public abstract CovidTrackingScreen navigateTo(String url);

    public abstract VisualElement findMetricByOcrText(String metricName);

    public abstract VisualElement selectStateByOcrText(String stateName);

    public abstract VisualElement findChartHeaderByImageTemplate(String imageTemplatePath);
}
