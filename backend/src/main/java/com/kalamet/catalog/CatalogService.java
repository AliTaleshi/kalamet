package com.kalamet.catalog;

import com.kalamet.catalog.CatalogDtos.BrandResponse;
import com.kalamet.catalog.CatalogDtos.CategoryDetail;
import com.kalamet.catalog.CatalogDtos.CategoryNode;
import com.kalamet.catalog.CatalogDtos.CategoryRef;
import com.kalamet.catalog.CatalogDtos.ImageResponse;
import com.kalamet.catalog.CatalogDtos.OptionResponse;
import com.kalamet.catalog.CatalogDtos.ProductDetail;
import com.kalamet.catalog.CatalogDtos.ProductSummary;
import com.kalamet.catalog.CatalogDtos.SpecGroup;
import com.kalamet.catalog.CatalogDtos.SpecResponse;
import com.kalamet.catalog.CatalogDtos.VariantResponse;
import com.kalamet.common.ApiException;
import com.kalamet.common.PageResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
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
    private final ProductSearch productSearch;
    private final Clock clock;

    CatalogService(CategoryRepository categories, BrandRepository brands, ProductRepository products,
                   ProductSearch productSearch, Clock clock) {
        this.categories = categories;
        this.brands = brands;
        this.products = products;
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

    public PageResponse<ProductSummary> search(ProductSearch.Criteria criteria, int page, int size) {
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
