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
}
