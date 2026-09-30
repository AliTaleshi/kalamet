package com.kalamet.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Request and response bodies of the catalog. All money values are Rial. */
public final class CatalogDtos {

    static final String SLUG_PATTERN = "^[a-z0-9]+(?:-[a-z0-9]+)*$";
    static final String SLUG_MESSAGE = "نامک فقط شامل حروف کوچک انگلیسی، عدد و خط تیره است.";

    private CatalogDtos() {
    }

    // --- Storefront ---

    public record CategoryNode(Long id, String name, String slug, String imageUrl, List<CategoryNode> children) {
    }

    public record CategoryRef(Long id, String name, String slug) {

        static CategoryRef of(Category category) {
            return new CategoryRef(category.getId(), category.getName(), category.getSlug());
        }
    }

    public record CategoryDetail(Long id, String name, String slug, String imageUrl,
                                 List<CategoryRef> breadcrumbs, List<CategoryRef> children) {
    }

    public record BrandResponse(Long id, String name, String nameEn, String slug, String logoUrl) {

        static BrandResponse of(Brand brand) {
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

        static ImageResponse of(ProductImage image) {
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

    // --- Admin ---

    public record CategoryRequest(
            Long parentId,
            @NotBlank(message = "نام دسته‌بندی را وارد کنید.") @Size(max = 100) String name,
            @NotBlank @Size(max = 120) @Pattern(regexp = SLUG_PATTERN, message = SLUG_MESSAGE) String slug,
            @Size(max = 500) String imageUrl,
            Integer sortOrder) {
    }

    public record BrandRequest(
            @NotBlank(message = "نام برند را وارد کنید.") @Size(max = 100) String name,
            @Size(max = 100) String nameEn,
            @NotBlank @Size(max = 120) @Pattern(regexp = SLUG_PATTERN, message = SLUG_MESSAGE) String slug,
            @Size(max = 500) String logoUrl) {
    }

    public record SpecRequest(@Size(max = 100) String groupName,
                              @NotBlank @Size(max = 100) String name,
                              @NotBlank @Size(max = 500) String value) {
    }

    /** {@code sortOrder} 0 is the main image; defaults to 0. */
    public record ImageRequest(@NotBlank @Size(max = 500) String url, @Size(max = 255) String altText,
                               Long variantId, Integer sortOrder) {
    }

    /**
     * {@code stock} replaces the current stock. Send back the {@code version} you read (from the
     * admin product detail) so that sales made meanwhile are not overwritten: a stale version is
     * rejected with 409. Without it the update is applied unconditionally.
     */
    public record VariantRequest(
            @NotBlank(message = "کد انبار (SKU) را وارد کنید.") @Size(max = 64) String sku,
            Map<String, String> attributes,
            @NotNull(message = "قیمت را وارد کنید.") @Min(value = 0, message = "قیمت نمی‌تواند منفی باشد.") Long price,
            Long compareAtPrice,
            Instant discountEndsAt,
            @NotNull(message = "موجودی را وارد کنید.")
            @Min(value = 0, message = "موجودی نمی‌تواند منفی باشد.") @Max(1_000_000) Integer stock,
            Boolean active,
            Long version) {
    }

    public record ProductRequest(
            @NotNull(message = "دسته‌بندی را انتخاب کنید.") Long categoryId,
            Long brandId,
            @NotBlank(message = "نام کالا را وارد کنید.") @Size(max = 250) String name,
            @Size(max = 250) String nameEn,
            @NotBlank @Size(max = 250) @Pattern(regexp = SLUG_PATTERN, message = SLUG_MESSAGE) String slug,
            String description,
            Boolean active) {
    }

    /** Creates a product with everything it needs to be sold. */
    public record ProductCreateRequest(
            @Valid @NotNull ProductRequest product,
            @Valid @NotEmpty(message = "کالا باید دست‌کم یک تنوع داشته باشد.") List<VariantRequest> variants,
            @Valid List<SpecRequest> specs,
            @Valid List<ImageRequest> images) {
    }

    public record AdminVariantResponse(Long id, String sku, Map<String, String> attributes, long price,
                                       Long compareAtPrice, Instant discountEndsAt, int stock, boolean active,
                                       long version) {

        static AdminVariantResponse of(ProductVariant v) {
            return new AdminVariantResponse(v.getId(), v.getSku(), v.getAttributes(), v.getPrice(),
                    v.getCompareAtPrice(), v.getDiscountEndsAt(), v.getStock(), v.isActive(), v.getVersion());
        }
    }

    public record AdminProductSummary(Long id, String name, String slug, String categoryName, String brandName,
                                      boolean active, Instant createdAt) {

        static AdminProductSummary of(Product p) {
            return new AdminProductSummary(p.getId(), p.getName(), p.getSlug(), p.getCategory().getName(),
                    p.getBrand() == null ? null : p.getBrand().getName(), p.isActive(), p.getCreatedAt());
        }
    }

    public record AdminProductDetail(Long id, CategoryRef category, BrandResponse brand, String name, String nameEn,
                                     String slug, String description, boolean active, Instant createdAt,
                                     Instant updatedAt, List<AdminVariantResponse> variants,
                                     List<SpecRequest> specs, List<ImageResponse> images) {
    }

    public record LowStockResponse(Long variantId, String sku, Long productId, String productName, int stock) {
    }
}
