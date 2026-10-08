package com.znsio.teswiz.screen.android.visualocr;

import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.Visual;
import com.znsio.teswiz.screen.visualocr.AbstractVisualOcrScreen;

public class VisualOcrScreenAndroid extends AbstractVisualOcrScreen {

    public VisualOcrScreenAndroid(Driver driver, Visual visually) {
        super(driver, visually);
    }

    @Override
    protected void afterTextEntry() {
        driver.hideKeyboard();
    }
}
