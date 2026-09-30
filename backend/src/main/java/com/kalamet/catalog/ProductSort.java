package com.kalamet.catalog;

import com.kalamet.common.ApiException;
import java.util.Locale;

/** Listing sort orders; the query parameter is the lower-case, dash-separated name. */
public enum ProductSort {
    NEWEST("p.created_at DESC"),
    CHEAPEST("best.price ASC"),
    MOST_EXPENSIVE("best.price DESC"),
    BIGGEST_DISCOUNT("CASE WHEN best.original_price > 0 "
            + "THEN (best.original_price - best.price)::numeric / best.original_price ELSE 0 END DESC"),
    TOP_RATED("r.rating DESC NULLS LAST, r.review_count DESC NULLS LAST"),
    BESTSELLING("s.sold DESC NULLS LAST");

    final String orderBy;

    ProductSort(String orderBy) {
        this.orderBy = orderBy;
    }

    public static ProductSort parse(String value) {
        if (value == null || value.isBlank()) {
            return NEWEST;
        }
        try {
            return valueOf(value.strip().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("INVALID_SORT",
                    "مرتب‌سازی نامعتبر است. مقادیر مجاز: newest, cheapest, most-expensive, biggest-discount, "
                            + "top-rated, bestselling.");
        }
    }
}
