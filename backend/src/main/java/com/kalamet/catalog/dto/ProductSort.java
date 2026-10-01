package com.kalamet.catalog.dto;

import com.kalamet.common.error.ApiException;
import java.util.Locale;

/** Listing sort orders; the query parameter is the lower-case, dash-separated name. */
public enum ProductSort {
    NEWEST,
    CHEAPEST,
    MOST_EXPENSIVE,
    BIGGEST_DISCOUNT,
    TOP_RATED,
    BESTSELLING;

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
