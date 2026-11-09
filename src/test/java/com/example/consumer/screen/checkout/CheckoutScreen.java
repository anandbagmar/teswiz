package com.example.consumer.screen.checkout;

/**
 * Test fixture: a screen contract declared in a consumer project's own namespace, outside
 * {@code com.znsio.teswiz.screen}, as {@link com.znsio.teswiz.screen.ScreenPackageConvention} allows.
 */
public abstract class CheckoutScreen {
    public abstract CheckoutScreen applyCoupon(String couponCode);
}
