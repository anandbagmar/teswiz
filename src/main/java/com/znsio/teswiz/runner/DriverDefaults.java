package com.znsio.teswiz.runner;

import com.znsio.teswiz.config.TeswizRuntimeConfiguration;

final class DriverDefaults {
    static int waitTimeoutSeconds() {
        return TeswizRuntimeConfiguration.getInt(TeswizRuntimeConfiguration.DRIVER_WAIT_TIMEOUT_SECONDS);
    }

    static int clickRetryAttempts() {
        return TeswizRuntimeConfiguration.getInt(TeswizRuntimeConfiguration.DRIVER_CLICK_RETRY_ATTEMPTS);
    }

    static int clickRetryDelaySeconds() {
        return TeswizRuntimeConfiguration.getInt(TeswizRuntimeConfiguration.DRIVER_CLICK_RETRY_DELAY_SECONDS);
    }

    static int scrollMaxAttempts() {
        return TeswizRuntimeConfiguration.getInt(TeswizRuntimeConfiguration.DRIVER_SCROLL_MAX_ATTEMPTS);
    }

    private DriverDefaults() {
    }
}
