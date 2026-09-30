package com.kalamet.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PriceQuoteTest {

    private static final Instant NOW = Instant.parse("2026-10-01T10:00:00Z");

    @Test
    void regularPriceWithoutDiscount() {
        PriceQuote quote = PriceQuote.of(variant(100_000, null, null), NOW);
        assertThat(quote.price()).isEqualTo(100_000);
        assertThat(quote.originalPrice()).isNull();
        assertThat(quote.discountPercent()).isZero();
        assertThat(quote.saving()).isZero();
    }

    @Test
    void runningOfferShowsCrossedOutPrice() {
        Instant endsAt = NOW.plus(Duration.ofHours(3));
        PriceQuote quote = PriceQuote.of(variant(89_000_000, 104_000_000L, endsAt), NOW);
        assertThat(quote.price()).isEqualTo(89_000_000);
        assertThat(quote.originalPrice()).isEqualTo(104_000_000);
        assertThat(quote.offerEndsAt()).isEqualTo(endsAt);
        assertThat(quote.discountPercent()).isEqualTo(14);   // 14.42% rounded down
        assertThat(quote.saving()).isEqualTo(15_000_000);
    }

    @Test
    void endedOfferFallsBackToRegularPrice() {
        PriceQuote quote = PriceQuote.of(variant(4_800_000, 5_500_000L, NOW), NOW);
        assertThat(quote.price()).isEqualTo(5_500_000);
        assertThat(quote.originalPrice()).isNull();
        assertThat(quote.offerEndsAt()).isNull();
    }

    @Test
    void permanentDiscountHasNoEndTime() {
        PriceQuote quote = PriceQuote.of(variant(12_900_000, 15_900_000L, null), NOW);
        assertThat(quote.price()).isEqualTo(12_900_000);
        assertThat(quote.discountPercent()).isEqualTo(18);
        assertThat(quote.offerEndsAt()).isNull();
    }

    private static ProductVariant variant(long price, Long compareAt, Instant endsAt) {
        ProductVariant variant = new ProductVariant();
        variant.setPrice(price);
        variant.setCompareAtPrice(compareAt);
        variant.setDiscountEndsAt(endsAt);
        return variant;
    }
}
