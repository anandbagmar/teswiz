package com.znsio.teswiz.web.provider.selenium;

import com.znsio.teswiz.config.TeswizConfig;
import java.util.Map;

import com.znsio.teswiz.entities.Platform;
import com.znsio.teswiz.runner.Setup;
import com.znsio.teswiz.tools.JsonFile;

final class SeleniumWebPlatformCapabilityLoader {
    Map load() {
        String capabilityFile = TeswizConfig.reader().getString(Setup.CAPS);
        Map<String, Map> loadedCapabilityFile = JsonFile.loadJsonFile(capabilityFile);
        return loadedCapabilityFile.get(Platform.web.name());
    }
}
