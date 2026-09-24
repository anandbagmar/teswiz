package com.znsio.teswiz.screen.parity;

import com.znsio.teswiz.screen.ScreenRegistry;

public abstract class WebEngineParityScreen {
    public static WebEngineParityScreen get() {
        return ScreenRegistry.getScreen(WebEngineParityScreen.class);
    }

    public abstract WebEngineParityScreen addCookie(String key, String value);

    public abstract boolean isCookiePresent(String key);

    public abstract WebEngineParityScreen deleteCookie(String key);

    public abstract WebEngineParityScreen setViewport(int width, int height);

    public abstract int[] getViewportSize();

    public abstract Object executeAsyncScript(int delayMs, String returnVal);
}
