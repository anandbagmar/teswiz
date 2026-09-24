package com.znsio.teswiz.web.playwright;

import com.microsoft.playwright.Frame;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public interface PlaywrightSearchRoot {
    Locator locator(String selector);

    Object evaluate(String expression, Object arg);

    final class PageSearchRoot implements PlaywrightSearchRoot {
        private final Page page;

        public PageSearchRoot(Page page) {
            this.page = page;
        }

        @Override
        public Locator locator(String selector) {
            return page.locator(selector);
        }

        @Override
        public Object evaluate(String expression, Object arg) {
            return page.evaluate(expression, arg);
        }

        public Page page() {
            return page;
        }
    }

    final class FrameSearchRoot implements PlaywrightSearchRoot {
        private final Frame frame;

        public FrameSearchRoot(Frame frame) {
            this.frame = frame;
        }

        @Override
        public Locator locator(String selector) {
            return frame.locator(selector);
        }

        @Override
        public Object evaluate(String expression, Object arg) {
            return frame.evaluate(expression, arg);
        }

        public Frame frame() {
            return frame;
        }
    }

    final class FrameLocatorSearchRoot implements PlaywrightSearchRoot {
        private final FrameLocator frameLocator;

        public FrameLocatorSearchRoot(FrameLocator frameLocator) {
            this.frameLocator = frameLocator;
        }

        @Override
        public Locator locator(String selector) {
            return frameLocator.locator(selector);
        }

        @Override
        public Object evaluate(String expression, Object arg) {
            return frameLocator.locator(":root").evaluate(expression, arg);
        }
    }
}
