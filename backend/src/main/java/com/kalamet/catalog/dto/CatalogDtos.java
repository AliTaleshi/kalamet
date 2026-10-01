package com.kalamet.catalog.dto;

import com.kalamet.catalog.domain.Brand;
import com.kalamet.catalog.domain.Category;
import com.kalamet.catalog.domain.ProductImage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Storefront catalog responses. All money values are Rial. */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record CategoryNode(Long id, String name, String slug, String imageUrl, List<CategoryNode> children) {
    }

    public record CategoryRef(Long id, String name, String slug) {

        public static CategoryRef of(Category category) {
            return new CategoryRef(category.getId(), category.getName(), category.getSlug());
        }
    }

    public record CategoryDetail(Long id, String name, String slug, String imageUrl,
                                 List<CategoryRef> breadcrumbs, List<CategoryRef> children) {
    }

    public record BrandResponse(Long id, String name, String nameEn, String slug, String logoUrl) {

        public static BrandResponse of(Brand brand) {
            return brand == null ? null : new BrandResponse(brand.getId(), brand.getName(), brand.getNameEn(),
                    brand.getSlug(), brand.getLogoUrl());
        }
    }

    /** A product card in listings: the price shown is its cheapest in-stock variant. */
    public record ProductSummary(Long id, String slug, String name, String nameEn, String brandName, String imageUrl,
                                 long price, Long originalPrice, int discountPercent, Instant offerEndsAt,
                                 boolean inStock, BigDecimal rating, long reviewCount) {
    }

    public record ImageResponse(Long id, String url, String altText, Long variantId) {

        public static ImageResponse of(ProductImage image) {
            return new ImageResponse(image.getId(), image.getUrl(), image.getAltText(),
                    image.getVariant() == null ? null : image.getVariant().getId());
        }
    }

    /**
     * {@code remaining} is set only when stock is low (the "only 2 left" hint), so the
     * exact stock level of well-stocked items stays private.
     */
    public record VariantResponse(Long id, String sku, Map<String, String> attributes, long price,
                                  Long originalPrice, int discountPercent, Instant offerEndsAt,
                                  boolean inStock, Integer remaining) {
    }

    /** One selectable option on the product page, e.g. key "color" with values مشکی and سفید. */
    public record OptionResponse(String key, List<String> values) {
    }

    public record SpecResponse(String name, String value) {
    }

    public record SpecGroup(String name, List<SpecResponse> specs) {
    }

    public record RatingSummary(BigDecimal average, long count, Integer recommendedPercent,
                                Map<Integer, Long> distribution) {
    }

    public record ProductDetail(Long id, String slug, String name, String nameEn, String description,
                                BrandResponse brand, List<CategoryRef> breadcrumbs, List<ImageResponse> images,
                                List<OptionResponse> options, List<VariantResponse> variants,
                                List<SpecGroup> specGroups, RatingSummary rating) {
    }
}
