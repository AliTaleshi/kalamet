package com.kalamet.catalog.domain;

import java.time.Instant;

/**
 * What a variant costs right now.
 *
 * @param price         amount charged, Rial
 * @param originalPrice crossed-out price, or null when there is no discount
 * @param offerEndsAt   end of a running "amazing offer", or null
 */
public record PriceQuote(long price, Long originalPrice, Instant offerEndsAt) {

    /**
     * The pricing rule, shared by the product page, the cart and checkout (ProductSearch
     * repeats it in SQL): when an offer's end time has passed, the variant goes back to its
     * regular price, which is the stored compare-at price.
     */
    public static PriceQuote of(ProductVariant variant, Instant now) {
        Instant endsAt = variant.getDiscountEndsAt();
        if (endsAt != null && !now.isBefore(endsAt)) {
            return new PriceQuote(variant.getCompareAtPrice(), null, null);
        }
        return new PriceQuote(variant.getPrice(), variant.getCompareAtPrice(), endsAt);
    }

    /** Whole percent off, rounded down so the badge never overstates the saving. */
    public int discountPercent() {
        return discountPercent(price, originalPrice);
    }

    public long saving() {
        return originalPrice == null ? 0 : originalPrice - price;
    }

    public static int discountPercent(long price, Long originalPrice) {
        if (originalPrice == null || originalPrice <= 0 || originalPrice <= price) {
            return 0;
        }
        return (int) ((originalPrice - price) * 100 / originalPrice);
    }
}
