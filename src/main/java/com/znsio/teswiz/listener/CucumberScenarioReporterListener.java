package com.znsio.teswiz.listener;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.epam.reportportal.cucumber.ScenarioReporter;
import com.znsio.teswiz.runner.AppiumServerManager;

public class CucumberScenarioReporterListener extends ScenarioReporter {

    private static final Logger LOGGER = LogManager.getLogger(
            CucumberScenarioReporterListener.class.getName());
    public AppiumServerManager appiumServerManager;

    private static final Map<String, String> MIME_TYPES_EXTENSIONS =
            new HashMap<>() {
                {
                    this.put("image/bmp", "bmp");
                    this.put("image/gif", "gif");
                    this.put("image/jpeg", "jpg");
                    this.put("image/png", "png");
                    this.put("image/svg+xml", "svg");
                    this.put("video/ogg", "ogg");
                }
            };

    public CucumberScenarioReporterListener() throws Exception {
        LOGGER.info("CucumberScenarioReporterListener");
        appiumServerManager = new AppiumServerManager();
    }

}
