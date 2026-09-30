package com.znsio.teswiz.screen.web.visualocr;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.Visual;
import com.znsio.teswiz.runner.VisualElement;
import com.znsio.teswiz.screen.visualocr.VisualOcrScreen;

public class VisualOcrScreenWeb extends VisualOcrScreen {
    private static final String SCREEN_NAME = VisualOcrScreenWeb.class.getSimpleName();
    private static final Logger LOGGER = LogManager.getLogger(SCREEN_NAME);

    private final Driver driver;
    private final Visual visually;

    public VisualOcrScreenWeb(Driver driver, Visual visually) {
        this.driver = driver;
        this.visually = visually;
        LOGGER.info("Initialized " + SCREEN_NAME);
    }

    @Override
    public VisualElement findVisualElementByText(String text) {
        LOGGER.info("Finding visual element by text: " + text);
        return driver.findByText(text);
    }

    @Override
    public VisualElement findVisualElementByImage(List<String> imagePaths) {
        LOGGER.info("Finding visual element by image: " + imagePaths);
        return driver.findByImage(imagePaths);
    }

    @Override
    public VisualElement findVisualElementByTextOrImage(String text, List<String> imagePaths) {
        LOGGER.info("Finding visual element by text or image: " + text + " / " + imagePaths);
        return driver.findByTextOrImage(text, imagePaths);
    }

    @Override
    public VisualElement findVisualElementByImageOrText(List<String> imagePaths, String text) {
        LOGGER.info("Finding visual element by image or text: " + imagePaths + " / " + text);
        return driver.findByImageOrText(imagePaths, text);
    }

    @Override
    public List<VisualElement> findAllVisualElementsByText(String text) {
        LOGGER.info("Finding all visual elements by text: " + text);
        return driver.findAllByText(text);
    }

    @Override
    public List<VisualElement> findAllVisualElementsByImage(List<String> imagePaths) {
        LOGGER.info("Finding all visual elements by image: " + imagePaths);
        return driver.findAllByImage(imagePaths);
    }

    @Override
    public List<VisualElement> findAllVisualElementsByTextOrImage(String text, List<String> imagePaths) {
        LOGGER.info("Finding all visual elements by text or image: " + text + " / " + imagePaths);
        return driver.findAllByTextOrImage(text, imagePaths);
    }

    @Override
    public List<VisualElement> findAllVisualElementsByImageOrText(List<String> imagePaths, String text) {
        LOGGER.info("Finding all visual elements by image or text: " + imagePaths + " / " + text);
        return driver.findAllByImageOrText(imagePaths, text);
    }


    @Override
    public VisualElement findVisualElementRelativeByText(String targetText, com.znsio.teswiz.entities.SpatialDirection direction, String anchorText) {
        LOGGER.info(String.format("Finding visual element '%s' %s anchor text '%s'", targetText, direction.getDirection(), anchorText));
        return driver.findRelativeByText(targetText, direction, anchorText);
    }

    @Override
    public VisualElement findVisualElementRelativeByImage(List<String> imagePaths, com.znsio.teswiz.entities.SpatialDirection direction, String anchorText) {
        LOGGER.info(String.format("Finding visual element matching image %s %s anchor text '%s'", imagePaths, direction.getDirection(), anchorText));
        return driver.findRelativeByImage(imagePaths, direction, anchorText);
    }

    @Override
    public VisualElement findVisualElementByTextInRegion(String text, com.znsio.teswiz.entities.VisualRegion region) {
        LOGGER.info(String.format("Finding visual element '%s' in region %s", text, region));
        return driver.findByText(text, region);
    }

    @Override
    public VisualElement findVisualElementByImageInRegion(List<String> imagePaths, com.znsio.teswiz.entities.VisualRegion region) {
        LOGGER.info(String.format("Finding visual element matching image %s in region %s", imagePaths, region));
        return driver.findByImage(imagePaths, region);
    }
}

