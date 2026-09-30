package com.kalamet.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Cart bodies. Money in Rial. */
public final class CartDtos {

    private CartDtos() {
    }

    public record AddItemRequest(@NotNull Long variantId,
                                 @NotNull @Min(value = 1, message = "تعداد باید دست‌کم ۱ باشد.") @Max(100) Integer quantity) {
    }

    /** 0 removes the item. */
    public record QuantityRequest(@NotNull @Min(0) @Max(100) Integer quantity) {
    }

    /** Items collected before sign-in; quantities are capped to what can be bought. */
    public record MergeRequest(@NotNull @Size(max = 100) List<@Valid AddItemRequest> items) {
    }

    public enum LineIssue {
        /** The product or variant is no longer sold. */
        UNAVAILABLE,
        OUT_OF_STOCK,
        /** Fewer units in stock than in the cart. */
        INSUFFICIENT_STOCK
    }

    public record CartLine(Long variantId, String sku, String productSlug, String productName, String imageUrl,
                           Map<String, String> attributes, int quantity, long unitPrice, Long originalPrice,
                           int discountPercent, Instant offerEndsAt, long lineTotal, int maxQuantity,
                           LineIssue issue) {
    }

    /**
     * Totals cover only the lines without an issue; checkout is refused while any line has one.
     *
     * @param discountTotal saving against crossed-out prices
     * @param payable       itemsTotal + shippingFee
     */
    public record CartView(List<CartLine> lines, int itemCount, long itemsTotal, long discountTotal,
                           long shippingFee, long freeShippingRemaining, long payable, boolean hasIssues) {
    }
}
