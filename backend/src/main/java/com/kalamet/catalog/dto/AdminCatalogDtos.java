package com.kalamet.catalog.dto;

import com.kalamet.catalog.domain.Product;
import com.kalamet.catalog.domain.ProductVariant;
import com.kalamet.catalog.dto.CatalogDtos.BrandResponse;
import com.kalamet.catalog.dto.CatalogDtos.CategoryRef;
import com.kalamet.catalog.dto.CatalogDtos.ImageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Admin catalog request and response bodies. All money values are Rial. */
public final class AdminCatalogDtos {

    private static final String SLUG_PATTERN = "^[a-z0-9]+(?:-[a-z0-9]+)*$";
    private static final String SLUG_MESSAGE = "نامک فقط شامل حروف کوچک انگلیسی، عدد و خط تیره است.";

    private AdminCatalogDtos() {
    }

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

        public static AdminVariantResponse of(ProductVariant v) {
            return new AdminVariantResponse(v.getId(), v.getSku(), v.getAttributes(), v.getPrice(),
                    v.getCompareAtPrice(), v.getDiscountEndsAt(), v.getStock(), v.isActive(), v.getVersion());
        }
    }

    public record AdminProductSummary(Long id, String name, String slug, String categoryName, String brandName,
                                      boolean active, Instant createdAt) {

        public static AdminProductSummary of(Product p) {
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
