package com.example.consumer.screen.checkout;

/**
 * Test fixture: a concrete value type that lives next to a consumer's screen contracts. It is
 * not a screen contract, so a playwright-ts action returning it must not be resolved as a screen.
 */
public final class CheckoutSummary {
    private final String total;

    public CheckoutSummary(String total) {
        this.total = total;
    }

    public String total() {
        return total;
    }
}
