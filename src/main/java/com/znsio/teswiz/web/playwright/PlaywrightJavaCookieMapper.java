package com.znsio.teswiz.web.playwright;

import org.openqa.selenium.Cookie;

import java.util.Date;

/**
 * Converts cookies between Selenium's {@link Cookie} and Playwright's
 * {@link com.microsoft.playwright.options.Cookie} for {@link PlaywrightJavaWebDriver}.
 */
final class PlaywrightJavaCookieMapper {
    private static final double MILLIS_PER_SECOND = 1000;

    private PlaywrightJavaCookieMapper() {
    }

    /**
     * Selenium expiry is a {@link Date}; Playwright expiry is seconds since the epoch. Unset domain, path and
     * expiry stay unset.
     */
    static com.microsoft.playwright.options.Cookie toPlaywrightCookie(Cookie cookie) {
        com.microsoft.playwright.options.Cookie playwrightCookie = new com.microsoft.playwright.options.Cookie(
                cookie.getName(), cookie.getValue());
        if (null != cookie.getDomain()) {
            playwrightCookie.setDomain(cookie.getDomain());
        }
        if (null != cookie.getPath()) {
            playwrightCookie.setPath(cookie.getPath());
        }
        if (null != cookie.getExpiry()) {
            playwrightCookie.setExpires(cookie.getExpiry().getTime() / MILLIS_PER_SECOND);
        }
        playwrightCookie.setSecure(cookie.isSecure());
        playwrightCookie.setHttpOnly(cookie.isHttpOnly());
        return playwrightCookie;
    }

    /** A missing Playwright expiry means a session cookie; missing secure/httpOnly flags mean {@code false}. */
    static Cookie toSeleniumCookie(com.microsoft.playwright.options.Cookie cookie) {
        return new Cookie.Builder(cookie.name, cookie.value).domain(cookie.domain).path(cookie.path)
                .expiresOn(null != cookie.expires ? new Date((long) (cookie.expires * MILLIS_PER_SECOND)) : null)
                .isSecure(Boolean.TRUE.equals(cookie.secure)).isHttpOnly(Boolean.TRUE.equals(cookie.httpOnly))
                .build();
    }
}
