package com.znsio.teswiz.screen.web.transportnsw;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;

import com.znsio.teswiz.entities.VisionStrategy;
import com.znsio.teswiz.entities.VisualRegion;
import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.Visual;
import com.znsio.teswiz.runner.VisualElement;
import com.znsio.teswiz.screen.transportnsw.TransportNswOcrScreen;

public class TransportNswOcrScreenWeb extends TransportNswOcrScreen {
    private static final String SCREEN_NAME = TransportNswOcrScreenWeb.class.getSimpleName();
    private static final Logger LOGGER = LogManager.getLogger(SCREEN_NAME);

    // Anchor the region on the current route value ("Tallawong to Bankstown"), which sits inside the
    // "Select route" dropdown field. Deriving the region from this element (with padding) frames the
    // actual dropdown for the vision locate, rather than the panel heading. A broad ancestor of the
    // heading was previously used but resolved to just the heading strip (above the dropdown).
    private static final By ROUTE_SELECTOR_FIELD = By.xpath(
            "//*[contains(normalize-space(.), 'Tallawong to Bankstown')]"
                    + "[not(.//*[contains(normalize-space(.), 'Tallawong to Bankstown')])]");
    // Padding (px) added around the anchor element so the dropdown field and its label are fully
    // inside the region handed to the vision locator.
    private static final int REGION_PADDING = 40;
    // An opened select/dropdown options list. Covers Angular Material overlay panels, ARIA listboxes,
    // and a native <select> in an expanded state - whichever the site uses, an open list matches one.
    private static final By ROUTE_SELECTOR_OPTIONS_LIST = By.xpath(
            "//*[@role='listbox' or contains(@class,'mat-mdc-select-panel') or contains(@class,'cdk-overlay-pane')]"
                    + "[.//*[@role='option'] or .//li or .//option]");
    private static final int OPTIONS_LIST_WAIT_SECONDS = 5;

    private final Driver driver;
    private final Visual visually;

    public TransportNswOcrScreenWeb(Driver driver, Visual visually) {
        this.driver = driver;
        this.visually = visually;
        LOGGER.info("Initialized " + SCREEN_NAME);
    }

    @Override
    public TransportNswOcrScreen navigateTo(String url) {
        LOGGER.info("Navigating to Transport NSW Metro page: " + url);
        driver.getInnerDriver().get(url);
        return this;
    }

    @Override
    public TransportNswOcrScreen scrollToExploreRouteMap() {
        LOGGER.info("Scrolling down to 'Explore the new route' interactive map section");
        By exploreSectionLocator = By.xpath("//*[contains(text(), 'Explore the new route')]");
        if (driver.isElementPresent(exploreSectionLocator)) {
            org.openqa.selenium.WebElement element = driver.findElement(exploreSectionLocator);
            ((org.openqa.selenium.JavascriptExecutor) driver.getInnerDriver())
                    .executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", element);
        } else {
            LOGGER.warn("'Explore the new route' text element not found; performing moderate window scroll");
            ((org.openqa.selenium.JavascriptExecutor) driver.getInnerDriver())
                    .executeScript("window.scrollBy(0, 400);");
        }
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        visually.checkWindow(SCREEN_NAME, "'Explore the new route' map section");
        return this;
    }

    @Override
    public VisualElement clickElementByImage(String elementName, String imagePath) {
        LOGGER.info("Finding and clicking '{}' using image template: {}", elementName, imagePath);
        VisualElement visualElement = visually.vision(VisionStrategy.OCR).findByImage(List.of(imagePath));
        visually.checkWindow(SCREEN_NAME, "Before clicking: " + elementName);
        if (visualElement != null) {
            visualElement.click();
        }
        visually.checkWindow(SCREEN_NAME, "After clicking: " + elementName);
        return visualElement;
    }

    @Override
    public VisualElement clickElementByOcrText(String elementName, String ocrText) {
        LOGGER.info("Finding and clicking '{}' using OCR text: {}", elementName, ocrText);
        VisualElement stationElement = visually.vision(VisionStrategy.OCR).findByText(ocrText);
        visually.checkWindow(SCREEN_NAME, "Before clicking: " + elementName);
        if (stationElement != null) {
            stationElement.click();
        }
        visually.checkWindow(SCREEN_NAME, "After clicking: " + elementName);
        return stationElement;
    }

    @Override
    public VisualElement clickElementByOcrThenLlm(String elementName, String description) {
        LOGGER.info("Finding and clicking '{}' using OCR_THEN_LLM with description: {}", elementName, description);
        VisualElement element = visually.vision(VisionStrategy.OCR_THEN_LLM).findByText(description);
        visually.checkWindow(SCREEN_NAME, "Before clicking (OCR_THEN_LLM): " + elementName);
        if (element != null) {
            element.click();
        }
        visually.checkWindow(SCREEN_NAME, "After clicking (OCR_THEN_LLM): " + elementName);
        return element;
    }

    @Override
    public VisualElement clickElementByLlm(String elementName, String description) {
        LOGGER.info("Finding and clicking '{}' using LLM with description: {}", elementName, description);
        VisualElement element = visually.vision(VisionStrategy.LLM).findByText(description);
        visually.checkWindow(SCREEN_NAME, "Before clicking (LLM): " + elementName);
        if (element != null) {
            element.click();
        }
        visually.checkWindow(SCREEN_NAME, "After clicking (LLM): " + elementName);
        return element;
    }

    @Override
    public VisualElement clickElementInRouteMapPanel(String elementName, VisionStrategy strategy, String description) {
        LOGGER.info("Finding and clicking '{}' within the Route Map panel using {} with description: {}",
                elementName, strategy, description);
        VisualRegion panelRegion = resolveRouteMapPanelRegion();
        LOGGER.info("Route Map panel region for the visual locate: {}", panelRegion);
        VisualElement element = visually.vision(strategy).findByText(description, panelRegion);
        visually.checkWindow(SCREEN_NAME, "Before clicking (" + strategy + ", Route Map panel): " + elementName);
        if (element != null) {
            element.click();
        }
        visually.checkWindow(SCREEN_NAME, "After clicking (" + strategy + ", Route Map panel): " + elementName);
        return element;
    }

    @Override
    public boolean isRouteSelectorOptionsListDisplayed() {
        LOGGER.info("Verifying the route selector options list is displayed");
        boolean displayed = driver.isElementPresentWithin(ROUTE_SELECTOR_OPTIONS_LIST, OPTIONS_LIST_WAIT_SECONDS);
        LOGGER.info("Route selector options list displayed? {}", displayed);
        visually.checkWindow(SCREEN_NAME, "Route selector options list displayed: " + displayed);
        return displayed;
    }

    /**
     * Builds a {@link VisualRegion} bounding the "Route Map" panel in VIEWPORT coordinates, so the
     * vision locate is constrained to that panel. The vision engine crops the region from the
     * viewport screenshot, but {@code WebElement.getLocation()} returns DOCUMENT coordinates, so the
     * current scroll offset is subtracted to convert document-space to viewport-space (otherwise, on
     * a scrolled page, the region would point far below the screenshot and the crop would be empty).
     * Falls back to {@code null} (whole-screen search) if the panel cannot be resolved or is not in
     * the current viewport.
     */
    private VisualRegion resolveRouteMapPanelRegion() {
        if (!driver.isElementPresent(ROUTE_SELECTOR_FIELD)) {
            LOGGER.warn("Route selector field anchor not found; locating against the whole screen instead");
            return null;
        }
        try {
            org.openqa.selenium.WebElement field = driver.findElement(ROUTE_SELECTOR_FIELD);
            org.openqa.selenium.Point docLocation = field.getLocation();
            org.openqa.selenium.Dimension size = field.getSize();

            long scrollX = toLong(((org.openqa.selenium.JavascriptExecutor) driver.getInnerDriver())
                    .executeScript("return Math.round(window.pageXOffset || document.documentElement.scrollLeft || 0);"));
            long scrollY = toLong(((org.openqa.selenium.JavascriptExecutor) driver.getInnerDriver())
                    .executeScript("return Math.round(window.pageYOffset || document.documentElement.scrollTop || 0);"));

            // Convert document-space to viewport-space, then pad so the field + label are fully framed.
            int viewportX = (int) (docLocation.getX() - scrollX) - REGION_PADDING;
            int viewportY = (int) (docLocation.getY() - scrollY) - REGION_PADDING;
            int width = size.getWidth() + (2 * REGION_PADDING);
            int height = size.getHeight() + (2 * REGION_PADDING);
            VisualRegion region = VisualRegion.inRegion(viewportX, viewportY, width, height);
            LOGGER.info("Route selector field: document=({}, {}), size=({}x{}), scroll=({}, {}) -> padded viewport region {}",
                    docLocation.getX(), docLocation.getY(), size.getWidth(), size.getHeight(), scrollX, scrollY, region);
            return region;
        } catch (RuntimeException e) {
            LOGGER.warn("Could not derive route selector region ({}); locating against the whole screen", e.getMessage());
            return null;
        }
    }

    private long toLong(Object jsNumber) {
        return (jsNumber instanceof Number) ? ((Number) jsNumber).longValue() : 0L;
    }

    @Override
    public VisualElement clickCalloutOptionByText(String text) {
        LOGGER.info("Finding and clicking callout option using OCR text: " + text);
        VisualElement calloutOption = visually.vision(VisionStrategy.OCR).findByText(text);
        visually.checkWindow(SCREEN_NAME, "Callout popup option: " + text);
        if (calloutOption != null) {
            calloutOption.click();
        }
        visually.checkWindow(SCREEN_NAME, "Clicked callout option: " + text);
        return calloutOption;
    }

    @Override
    public VisualElement clickStationNameOnMapByText(String stationName) {
        LOGGER.info("Finding and clicking station name on map using OCR text: " + stationName);
        VisualElement stationElement = visually.vision(VisionStrategy.OCR).findByText(stationName);
        visually.checkWindow(SCREEN_NAME, "Station map view: " + stationName);
        if (stationElement != null) {
            stationElement.click();
        }
        visually.checkWindow(SCREEN_NAME, "Clicked station name on map: " + stationName);
        return stationElement;
    }

    @Override
    public boolean isStationPageDisplayedFor(String stationName) {
        LOGGER.info("Verifying station page display for: " + stationName);
        String currentUrl = driver.getInnerDriver().getCurrentUrl();
        String pageTitle = driver.getInnerDriver().getTitle();
        if (currentUrl.toLowerCase().contains(stationName.toLowerCase())
                || pageTitle.toLowerCase().contains(stationName.toLowerCase())) {
            return true;
        }
        By stationTextLocator = By.xpath("//*[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + stationName.toLowerCase() + "')]");
        return driver.isElementPresent(stationTextLocator);
    }
}
