package com.kalamet.catalog.service;

import com.kalamet.catalog.domain.Brand;
import com.kalamet.catalog.domain.Category;
import com.kalamet.catalog.domain.Product;
import com.kalamet.catalog.domain.ProductImage;
import com.kalamet.catalog.domain.ProductSpec;
import com.kalamet.catalog.domain.ProductVariant;
import com.kalamet.catalog.dto.AdminCatalogDtos.AdminProductDetail;
import com.kalamet.catalog.dto.AdminCatalogDtos.AdminProductSummary;
import com.kalamet.catalog.dto.AdminCatalogDtos.AdminVariantResponse;
import com.kalamet.catalog.dto.AdminCatalogDtos.BrandRequest;
import com.kalamet.catalog.dto.AdminCatalogDtos.CategoryRequest;
import com.kalamet.catalog.dto.AdminCatalogDtos.ImageRequest;
import com.kalamet.catalog.dto.AdminCatalogDtos.LowStockResponse;
import com.kalamet.catalog.dto.AdminCatalogDtos.ProductCreateRequest;
import com.kalamet.catalog.dto.AdminCatalogDtos.ProductRequest;
import com.kalamet.catalog.dto.AdminCatalogDtos.SpecRequest;
import com.kalamet.catalog.dto.AdminCatalogDtos.VariantRequest;
import com.kalamet.catalog.dto.CatalogDtos.BrandResponse;
import com.kalamet.catalog.dto.CatalogDtos.CategoryRef;
import com.kalamet.catalog.dto.CatalogDtos.ImageResponse;
import com.kalamet.catalog.repository.BrandRepository;
import com.kalamet.catalog.repository.CategoryRepository;
import com.kalamet.catalog.repository.ProductImageRepository;
import com.kalamet.catalog.repository.ProductRepository;
import com.kalamet.catalog.repository.ProductVariantRepository;
import com.kalamet.common.error.ApiException;
import com.kalamet.common.text.PersianText;
import com.kalamet.common.web.PageResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Catalog management for admins. Products are never hard-deleted: deactivate them instead. */
@Service
@Transactional
public class AdminCatalogService {

    private static final Pattern ATTRIBUTE_KEY = Pattern.compile("^[a-z][a-z0-9_]{0,31}$");

    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final ProductImageRepository images;

    AdminCatalogService(CategoryRepository categories, BrandRepository brands, ProductRepository products,
                        ProductVariantRepository variants, ProductImageRepository images) {
        this.categories = categories;
        this.brands = brands;
        this.products = products;
        this.variants = variants;
        this.images = images;
    }

    // --- Categories ---

    public CategoryRef createCategory(CategoryRequest request) {
        Category category = new Category();
        applyCategory(category, request);
        return CategoryRef.of(categories.saveAndFlush(category));
    }

    public CategoryRef updateCategory(Long id, CategoryRequest request) {
        Category category = categories.findById(id).orElseThrow(CatalogService::categoryNotFound);
        applyCategory(category, request);
        return CategoryRef.of(categories.saveAndFlush(category));
    }

    public void deleteCategory(Long id) {
        Category category = categories.findById(id).orElseThrow(CatalogService::categoryNotFound);
        if (categories.existsByParentId(id)) {
            throw ApiException.conflict("CATEGORY_HAS_CHILDREN", "این دسته‌بندی زیرمجموعه دارد و حذف نمی‌شود.");
        }
        if (products.existsByCategoryId(id)) {
            throw ApiException.conflict("CATEGORY_HAS_PRODUCTS", "این دسته‌بندی کالا دارد و حذف نمی‌شود.");
        }
        categories.delete(category);
    }

    private void applyCategory(Category category, CategoryRequest request) {
        Category parent = null;
        if (request.parentId() != null) {
            parent = categories.findById(request.parentId())
                    .orElseThrow(() -> ApiException.badRequest("INVALID_PARENT", "دسته‌بندی والد پیدا نشد."));
            for (Category ancestor = parent; ancestor != null; ancestor = ancestor.getParent()) {
                if (category.getId() != null && ancestor.getId().equals(category.getId())) {
                    throw ApiException.badRequest("CATEGORY_CYCLE",
                            "یک دسته‌بندی نمی‌تواند زیرمجموعه خودش یا فرزندانش باشد.");
                }
            }
        }
        category.setParent(parent);
        category.setName(PersianText.normalize(request.name()));
        category.setSlug(request.slug());
        category.setImageUrl(PersianText.normalize(request.imageUrl()));
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    }

    // --- Brands ---

    public BrandResponse createBrand(BrandRequest request) {
        Brand brand = new Brand();
        applyBrand(brand, request);
        return BrandResponse.of(brands.saveAndFlush(brand));
    }

    public BrandResponse updateBrand(Long id, BrandRequest request) {
        Brand brand = brands.findById(id).orElseThrow(AdminCatalogService::brandNotFound);
        applyBrand(brand, request);
        return BrandResponse.of(brands.saveAndFlush(brand));
    }

    /** Products of a deleted brand keep existing without a brand (ON DELETE SET NULL). */
    public void deleteBrand(Long id) {
        brands.delete(brands.findById(id).orElseThrow(AdminCatalogService::brandNotFound));
    }

    private static void applyBrand(Brand brand, BrandRequest request) {
        brand.setName(PersianText.normalize(request.name()));
        brand.setNameEn(PersianText.normalize(request.nameEn()));
        brand.setSlug(request.slug());
        brand.setLogoUrl(PersianText.normalize(request.logoUrl()));
    }

    // --- Products ---

    @Transactional(readOnly = true)
    public PageResponse<AdminProductSummary> searchProducts(String query, Boolean active, Pageable pageable) {
        String normalized = query == null || query.isBlank() ? null : PersianText.normalize(query);
        return PageResponse.of(products.adminSearch(normalized, active, pageable), AdminProductSummary::of);
    }

    @Transactional(readOnly = true)
    public AdminProductDetail product(Long id) {
        return detail(requireProduct(id));
    }

    public AdminProductDetail createProduct(ProductCreateRequest request) {
        Product product = new Product();
        applyProduct(product, request.product());
        for (VariantRequest variantRequest : request.variants()) {
            ProductVariant variant = new ProductVariant();
            applyVariant(variant, variantRequest);
            product.addVariant(variant);
        }
        if (request.specs() != null) {
            replaceSpecs(product, request.specs());
        }
        products.saveAndFlush(product);
        if (request.images() != null) {
            for (ImageRequest imageRequest : request.images()) {
                product.addImage(newImage(product, imageRequest));
            }
            products.saveAndFlush(product);
        }
        return detail(product);
    }

    public AdminProductDetail updateProduct(Long id, ProductRequest request) {
        Product product = requireProduct(id);
        applyProduct(product, request);
        return detail(products.saveAndFlush(product));
    }

    public AdminProductDetail replaceSpecs(Long productId, List<SpecRequest> specs) {
        Product product = requireProduct(productId);
        replaceSpecs(product, specs);
        return detail(products.saveAndFlush(product));
    }

    public AdminProductDetail addImage(Long productId, ImageRequest request) {
        Product product = requireProduct(productId);
        product.addImage(newImage(product, request));
        return detail(products.saveAndFlush(product));
    }

    public void deleteImage(Long productId, Long imageId) {
        ProductImage image = images.findById(imageId)
                .filter(i -> i.getProduct().getId().equals(productId))
                .orElseThrow(() -> ApiException.notFound("IMAGE_NOT_FOUND", "تصویر پیدا نشد."));
        image.getProduct().getImages().remove(image);
    }

    public AdminVariantResponse addVariant(Long productId, VariantRequest request) {
        Product product = requireProduct(productId);
        ProductVariant variant = new ProductVariant();
        applyVariant(variant, request);
        product.addVariant(variant);
        products.saveAndFlush(product);
        return AdminVariantResponse.of(variant);
    }

    public AdminVariantResponse updateVariant(Long variantId, VariantRequest request) {
        ProductVariant variant = variants.findById(variantId)
                .orElseThrow(() -> ApiException.notFound("VARIANT_NOT_FOUND", "تنوع کالا پیدا نشد."));
        if (request.version() != null && request.version() != variant.getVersion()) {
            throw ApiException.conflict("STALE_VARIANT",
                    "این تنوع پس از بارگذاری فرم تغییر کرده است (مثلاً فروش رفته). صفحه را تازه کنید و دوباره ذخیره کنید.");
        }
        applyVariant(variant, request);
        return AdminVariantResponse.of(variants.saveAndFlush(variant));
    }

    @Transactional(readOnly = true)
    public List<LowStockResponse> lowStock(int threshold) {
        return variants.findLowStock(threshold).stream()
                .map(v -> new LowStockResponse(v.getId(), v.getSku(), v.getProduct().getId(),
                        v.getProduct().getName(), v.getStock()))
                .toList();
    }

    private Product requireProduct(Long id) {
        return products.findWithDetailsById(id).orElseThrow(CatalogService::productNotFound);
    }

    private void applyProduct(Product product, ProductRequest request) {
        product.setCategory(categories.findById(request.categoryId())
                .orElseThrow(() -> ApiException.badRequest("INVALID_CATEGORY", "دسته‌بندی پیدا نشد.")));
        product.setBrand(request.brandId() == null ? null : brands.findById(request.brandId())
                .orElseThrow(() -> ApiException.badRequest("INVALID_BRAND", "برند پیدا نشد.")));
        product.setName(PersianText.normalize(request.name()));
        product.setNameEn(PersianText.normalize(request.nameEn()));
        product.setSlug(request.slug());
        product.setDescription(PersianText.normalize(request.description()));
        if (request.active() != null) {
            product.setActive(request.active());
        }
    }

    /** Checks the same rules as the database constraints, with messages an admin can act on. */
    private static void applyVariant(ProductVariant variant, VariantRequest request) {
        if (request.compareAtPrice() != null && request.compareAtPrice() <= request.price()) {
            throw ApiException.badRequest("INVALID_COMPARE_AT_PRICE",
                    "قیمت قبل از تخفیف باید بیشتر از قیمت فروش باشد.");
        }
        if (request.discountEndsAt() != null && request.compareAtPrice() == null) {
            throw ApiException.badRequest("OFFER_NEEDS_DISCOUNT",
                    "برای پیشنهاد شگفت‌انگیز، قیمت قبل از تخفیف را وارد کنید.");
        }
        Map<String, String> attributes = new LinkedHashMap<>();
        if (request.attributes() != null) {
            request.attributes().forEach((key, value) -> {
                if (key == null || !ATTRIBUTE_KEY.matcher(key).matches()) {
                    throw ApiException.badRequest("INVALID_ATTRIBUTE_KEY",
                            "کلید ویژگی باید انگلیسی و با حروف کوچک باشد (مانند color یا size).");
                }
                String normalized = PersianText.normalize(value);
                if (normalized == null) {
                    throw ApiException.badRequest("INVALID_ATTRIBUTE_VALUE", "مقدار ویژگی «" + key + "» خالی است.");
                }
                attributes.put(key, normalized);
            });
        }
        variant.setSku(request.sku().strip());
        variant.setAttributes(attributes);
        variant.setPrice(request.price());
        variant.setCompareAtPrice(request.compareAtPrice());
        variant.setDiscountEndsAt(request.discountEndsAt());
        variant.setStock(request.stock());
        if (request.active() != null) {
            variant.setActive(request.active());
        }
    }

    private static void replaceSpecs(Product product, List<SpecRequest> specs) {
        product.getSpecs().clear();
        int order = 0;
        for (SpecRequest request : specs) {
            ProductSpec spec = new ProductSpec();
            spec.setGroupName(PersianText.normalize(request.groupName()));
            spec.setName(PersianText.normalize(request.name()));
            spec.setValue(PersianText.normalize(request.value()));
            spec.setSortOrder(++order);
            product.addSpec(spec);
        }
    }

    private static ProductImage newImage(Product product, ImageRequest request) {
        ProductImage image = new ProductImage();
        image.setUrl(request.url().strip());
        image.setAltText(PersianText.normalize(request.altText()));
        image.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        if (request.variantId() != null) {
            image.setVariant(product.getVariants().stream()
                    .filter(v -> request.variantId().equals(v.getId()))
                    .findFirst()
                    .orElseThrow(() -> ApiException.badRequest("INVALID_VARIANT", "تنوع انتخاب‌شده متعلق به این کالا نیست.")));
        }
        return image;
    }

    private static AdminProductDetail detail(Product p) {
        return new AdminProductDetail(p.getId(), CategoryRef.of(p.getCategory()), BrandResponse.of(p.getBrand()),
                p.getName(), p.getNameEn(), p.getSlug(), p.getDescription(), p.isActive(), p.getCreatedAt(),
                p.getUpdatedAt(),
                p.getVariants().stream().map(AdminVariantResponse::of).toList(),
                p.getSpecs().stream().map(s -> new SpecRequest(s.getGroupName(), s.getName(), s.getValue())).toList(),
                p.getImages().stream().map(ImageResponse::of).toList());
    }

    private static ApiException brandNotFound() {
        return ApiException.notFound("BRAND_NOT_FOUND", "برند پیدا نشد.");
    }
}
