package com.example.consumer.model;

/**
 * Test fixture: an abstract type in a consumer package with no {@code screen} segment, so it is
 * not a screen contract under {@link com.znsio.teswiz.screen.ScreenPackageConvention}.
 */
public abstract class OrderLine {
    public abstract String sku();
}
