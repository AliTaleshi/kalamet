package com.kalamet.cart.service;

import com.kalamet.config.KalametProperties;
import org.springframework.stereotype.Component;

/** Flat shipping fee, free from a threshold. Amounts in Rial. */
@Component
public class ShippingPolicy {

    private final KalametProperties.Shipping settings;

    ShippingPolicy(KalametProperties properties) {
        this.settings = properties.shipping();
    }

    public long fee(long itemsTotal) {
        if (itemsTotal <= 0 || itemsTotal >= settings.freeThreshold()) {
            return 0;
        }
        return settings.flatFee();
    }

    /** How much more to add for free shipping; 0 when it already applies. */
    public long remainingForFreeShipping(long itemsTotal) {
        return Math.max(0, settings.freeThreshold() - itemsTotal);
    }
}
