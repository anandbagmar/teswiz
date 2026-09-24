package com.znsio.teswiz.api;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Playwright;
import com.znsio.teswiz.runner.Setup;
import com.znsio.teswiz.tools.OverriddenVariable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PlaywrightApiManager {
    private static final Logger LOGGER = LogManager.getLogger(PlaywrightApiManager.class);
    private static final ThreadLocal<Playwright> THREAD_PLAYWRIGHT = new ThreadLocal<>();
    private static final ThreadLocal<APIRequestContext> THREAD_REQUEST_CONTEXT = new ThreadLocal<>();

    private PlaywrightApiManager() {
    }

    public static synchronized APIRequestContext getAPIRequestContext() {
        if (THREAD_REQUEST_CONTEXT.get() == null) {
            LOGGER.info("Initializing Playwright APIRequestContext for thread: {}", Thread.currentThread().getName());
            Playwright playwright = Playwright.create();
            THREAD_PLAYWRIGHT.set(playwright);

            APIRequest.NewContextOptions options = new APIRequest.NewContextOptions()
                    .setIgnoreHTTPSErrors(true);

            String proxyUrl = OverriddenVariable.getOverriddenStringValue(Setup.PROXY_URL, "");
            if (proxyUrl != null && !proxyUrl.trim().isEmpty()) {
                options.setProxy(proxyUrl);
            }

            APIRequestContext requestContext = playwright.request().newContext(options);
            THREAD_REQUEST_CONTEXT.set(requestContext);
        }
        return THREAD_REQUEST_CONTEXT.get();
    }

    public static synchronized void closeContextForCurrentThread() {
        APIRequestContext context = THREAD_REQUEST_CONTEXT.get();
        if (context != null) {
            try {
                LOGGER.info("Disposing Playwright APIRequestContext for thread: {}", Thread.currentThread().getName());
                context.dispose();
            } catch (Exception e) {
                LOGGER.warn("Error disposing Playwright APIRequestContext: {}", e.getMessage());
            } finally {
                THREAD_REQUEST_CONTEXT.remove();
            }
        }

        Playwright playwright = THREAD_PLAYWRIGHT.get();
        if (playwright != null) {
            try {
                playwright.close();
            } catch (Exception e) {
                LOGGER.warn("Error closing Playwright instance: {}", e.getMessage());
            } finally {
                THREAD_PLAYWRIGHT.remove();
            }
        }
    }
}
