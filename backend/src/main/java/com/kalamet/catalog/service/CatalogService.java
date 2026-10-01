package com.kalamet.catalog.service;

import com.kalamet.catalog.domain.Category;
import com.kalamet.catalog.domain.PriceQuote;
import com.kalamet.catalog.domain.Product;
import com.kalamet.catalog.domain.ProductImage;
import com.kalamet.catalog.domain.ProductSpec;
import com.kalamet.catalog.domain.ProductVariant;
import com.kalamet.catalog.domain.VariantImages;
import com.kalamet.catalog.dto.CatalogDtos.BrandResponse;
import com.kalamet.catalog.dto.CatalogDtos.CategoryDetail;
import com.kalamet.catalog.dto.CatalogDtos.CategoryNode;
import com.kalamet.catalog.dto.CatalogDtos.CategoryRef;
import com.kalamet.catalog.dto.CatalogDtos.ImageResponse;
import com.kalamet.catalog.dto.CatalogDtos.OptionResponse;
import com.kalamet.catalog.dto.CatalogDtos.ProductDetail;
import com.kalamet.catalog.dto.CatalogDtos.ProductSummary;
import com.kalamet.catalog.dto.CatalogDtos.SpecGroup;
import com.kalamet.catalog.dto.CatalogDtos.SpecResponse;
import com.kalamet.catalog.dto.CatalogDtos.VariantResponse;
import com.kalamet.catalog.dto.ProductQuery;
import com.kalamet.catalog.repository.BrandRepository;
import com.kalamet.catalog.repository.CategoryRepository;
import com.kalamet.catalog.repository.ProductImageRepository;
import com.kalamet.catalog.repository.ProductRepository;
import com.kalamet.catalog.repository.ProductSearch;
import com.kalamet.catalog.repository.ProductVariantRepository;
import com.kalamet.common.error.ApiException;
import com.kalamet.common.web.PageResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Storefront reads: categories, brands, listings and the product page. */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    /** Below this, the product page shows how many units are left. */
    static final int LOW_STOCK_HINT = 5;

    private static final List<String> OPTION_ORDER = List.of("color", "size");
    private static final List<String> SIZE_ORDER = List.of("XXS", "XS", "S", "M", "L", "XL", "XXL", "3XL", "4XL");

    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final ProductImageRepository images;
    private final ProductSearch productSearch;
    private final Clock clock;

    CatalogService(CategoryRepository categories, BrandRepository brands, ProductRepository products,
                   ProductVariantRepository variants, ProductImageRepository images, ProductSearch productSearch,
                   Clock clock) {
        this.categories = categories;
        this.brands = brands;
        this.products = products;
        this.variants = variants;
        this.images = images;
        this.productSearch = productSearch;
        this.clock = clock;
    }

    public List<CategoryNode> categoryTree() {
        List<Category> all = categories.findAll(Sort.by("sortOrder", "name"));
        return childrenOf(null, all);
    }

    public CategoryDetail category(String slug) {
        Category category = categories.findBySlug(slug).orElseThrow(CatalogService::categoryNotFound);
        List<CategoryRef> children = categories.findAll(Sort.by("sortOrder", "name")).stream()
                .filter(c -> c.getParent() != null && c.getParent().getId().equals(category.getId()))
                .map(CategoryRef::of)
                .toList();
        return new CategoryDetail(category.getId(), category.getName(), category.getSlug(), category.getImageUrl(),
                breadcrumbs(category), children);
    }

    public List<BrandResponse> brands(String categorySlug) {
        Set<Long> withProducts = new HashSet<>(productSearch.brandIdsWithProducts(categorySlug));
        return brands.findAll(Sort.by("name")).stream()
                .filter(brand -> categorySlug == null || withProducts.contains(brand.getId()))
                .map(BrandResponse::of)
                .toList();
    }

    public PageResponse<ProductSummary> search(ProductQuery criteria, int page, int size) {
        return productSearch.search(criteria, clock.instant(), page, size);
    }

    public ProductDetail product(String slug) {
        Product product = products.findBySlugAndActiveTrue(slug).orElseThrow(CatalogService::productNotFound);
        Instant now = clock.instant();

        List<ProductVariant> variants = product.getVariants().stream().filter(ProductVariant::isActive).toList();
        List<VariantResponse> variantResponses = variants.stream().map(v -> variantResponse(v, now)).toList();

        return new ProductDetail(product.getId(), product.getSlug(), product.getName(), product.getNameEn(),
                product.getDescription(), BrandResponse.of(product.getBrand()), breadcrumbs(product.getCategory()),
                product.getImages().stream().map(ImageResponse::of).toList(),
                options(variants), variantResponses, specGroups(product.getSpecs()),
                productSearch.ratingSummary(product.getId()));
    }

    // --- For other features (cart, orders, reviews) ---

    /** An active product by slug, or 404. */
    public Product activeProduct(String slug) {
        return products.findBySlugAndActiveTrue(slug).orElseThrow(CatalogService::productNotFound);
    }

    /** A variant with its product loaded, whatever its status. */
    public Optional<ProductVariant> variant(Long variantId) {
        return variants.findWithProductById(variantId);
    }

    public Map<Long, ProductVariant> variants(Collection<Long> variantIds) {
        if (variantIds.isEmpty()) {
            return Map.of();
        }
        return variants.findWithProductByIdIn(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));
    }

    /** Variant id to the picture of that variant (see {@link VariantImages}), for cart and order lines. */
    public Map<Long, String> imageUrls(Collection<ProductVariant> variants) {
        if (variants.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<ProductImage>> byProduct = images.findByProductIdIn(
                        variants.stream().map(v -> v.getProduct().getId()).distinct().toList()).stream()
                .collect(Collectors.groupingBy(image -> image.getProduct().getId()));
        Map<Long, String> urls = new HashMap<>();
        for (ProductVariant variant : variants) {
            String url = VariantImages.urlFor(variant, byProduct.getOrDefault(variant.getProduct().getId(), List.of()));
            if (url != null) {
                urls.put(variant.getId(), url);
            }
        }
        return urls;
    }

    static VariantResponse variantResponse(ProductVariant variant, Instant now) {
        PriceQuote quote = PriceQuote.of(variant, now);
        int stock = variant.getStock();
        return new VariantResponse(variant.getId(), variant.getSku(), variant.getAttributes(), quote.price(),
                quote.originalPrice(), quote.discountPercent(), quote.offerEndsAt(), stock > 0,
                stock > 0 && stock <= LOW_STOCK_HINT ? stock : null);
    }

    /**
     * Distinct values per attribute key. JSONB does not keep key order, so keys follow
     * {@link #OPTION_ORDER} (others alphabetically) and clothing sizes go from small to large.
     */
    private static List<OptionResponse> options(List<ProductVariant> variants) {
        Map<String, Set<String>> values = new TreeMap<>(Comparator
                .comparingInt((String key) -> OPTION_ORDER.contains(key) ? OPTION_ORDER.indexOf(key) : OPTION_ORDER.size())
                .thenComparing(Comparator.naturalOrder()));
        for (ProductVariant variant : variants) {
            variant.getAttributes().forEach((key, value) ->
                    values.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(value));
        }
        return values.entrySet().stream()
                .map(entry -> new OptionResponse(entry.getKey(), entry.getValue().stream()
                        .sorted(Comparator.comparingInt(CatalogService::sizeRank)
                                .thenComparing(Comparator.naturalOrder()))
                        .toList()))
                .toList();
    }

    private static int sizeRank(String value) {
        int rank = SIZE_ORDER.indexOf(value.toUpperCase(Locale.ROOT));
        return rank < 0 ? SIZE_ORDER.size() : rank;
    }

    private static List<SpecGroup> specGroups(List<ProductSpec> specs) {
        Map<String, List<SpecResponse>> groups = new LinkedHashMap<>();
        for (ProductSpec spec : specs) {
            String group = Objects.requireNonNullElse(spec.getGroupName(), "مشخصات کلی");
            groups.computeIfAbsent(group, g -> new ArrayList<>()).add(new SpecResponse(spec.getName(), spec.getValue()));
        }
        return groups.entrySet().stream().map(e -> new SpecGroup(e.getKey(), e.getValue())).toList();
    }

    /** Root first, the category itself last. */
    static List<CategoryRef> breadcrumbs(Category category) {
        List<CategoryRef> trail = new ArrayList<>();
        for (Category current = category; current != null; current = current.getParent()) {
            trail.addFirst(CategoryRef.of(current));
        }
        return trail;
    }

    private static List<CategoryNode> childrenOf(Long parentId, List<Category> all) {
        return all.stream()
                .filter(c -> Objects.equals(parentId, c.getParent() == null ? null : c.getParent().getId()))
                .sorted(Comparator.comparingInt(Category::getSortOrder))
                .map(c -> new CategoryNode(c.getId(), c.getName(), c.getSlug(), c.getImageUrl(),
                        childrenOf(c.getId(), all)))
                .toList();
    }

    static ApiException productNotFound() {
        return ApiException.notFound("PRODUCT_NOT_FOUND", "کالا پیدا نشد.");
    }

    static ApiException categoryNotFound() {
        return ApiException.notFound("CATEGORY_NOT_FOUND", "دسته‌بندی پیدا نشد.");
    }
}
