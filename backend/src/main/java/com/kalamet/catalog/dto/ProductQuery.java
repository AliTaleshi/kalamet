package com.kalamet.catalog.dto;

import java.util.List;

/**
 * Filters of a product listing.
 *
 * @param text     free text; every word must appear in the product or brand name
 * @param category category slug; products of its subcategories are included
 * @param brands   brand slugs (any of them)
 * @param minPrice Rial, compared with the price shown on the card
 * @param maxPrice Rial
 */
public record ProductQuery(String text, String category, List<String> brands, Long minPrice, Long maxPrice,
                           boolean inStockOnly, boolean offersOnly, ProductSort sort) {
}
