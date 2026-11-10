package com.znsio.teswiz.web.provider.selenium;

import com.znsio.teswiz.config.TeswizConfig;
import com.znsio.teswiz.runner.Setup;

final class SeleniumWebExecutionConfigResolver {
    private final SeleniumWebSessionNameResolver sessionNameResolver = new SeleniumWebSessionNameResolver();

    SeleniumWebExecutionConfig resolve() {
        return new SeleniumWebExecutionConfig(
                TeswizConfig.reader().getString(Setup.APP_NAME),
                TeswizConfig.reader().getString(Setup.LAUNCH_NAME),
                TeswizConfig.reader().getString(Setup.LOG_DIR),
                sessionNameResolver.resolve(),
                TeswizConfig.reader().getString(Setup.CLOUD_USERNAME),
                TeswizConfig.reader().getString(Setup.CLOUD_KEY),
                TeswizConfig.reader().getString(Setup.PROXY_URL),
                TeswizConfig.reader().getBoolean(Setup.CLOUD_USE_LOCAL_TESTING),
                TeswizConfig.reader().getBoolean(Setup.CLOUD_USE_PROXY));
    }
}
