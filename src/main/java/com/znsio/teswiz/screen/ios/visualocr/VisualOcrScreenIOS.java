package com.znsio.teswiz.screen.ios.visualocr;

import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.Visual;
import com.znsio.teswiz.screen.visualocr.AbstractVisualOcrScreen;

public class VisualOcrScreenIOS extends AbstractVisualOcrScreen {

    public VisualOcrScreenIOS(Driver driver, Visual visually) {
        super(driver, visually);
    }

    @Override
    protected void afterTextEntry() {
        driver.hideKeyboard();
    }
}
