package com.kalamet.catalog;

import com.kalamet.catalog.CatalogDtos.BrandResponse;
import com.kalamet.catalog.CatalogDtos.CategoryDetail;
import com.kalamet.catalog.CatalogDtos.CategoryNode;
import com.kalamet.catalog.CatalogDtos.ProductDetail;
import com.kalamet.catalog.CatalogDtos.ProductSummary;
import com.kalamet.common.PageResponse;
import com.kalamet.common.Paging;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Catalog")
@RestController
class CatalogController {

    private final CatalogService catalogService;

    CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Operation(summary = "Category tree for the menu")
    @GetMapping("/api/categories")
    List<CategoryNode> categories() {
        return catalogService.categoryTree();
    }

    @Operation(summary = "One category with its breadcrumbs and subcategories")
    @GetMapping("/api/categories/{slug}")
    CategoryDetail category(@PathVariable String slug) {
        return catalogService.category(slug);
    }

    @Operation(summary = "Brands, optionally only those with products in a category")
    @GetMapping("/api/brands")
    List<BrandResponse> brands(@RequestParam(required = false) String category) {
        return catalogService.brands(category);
    }

    @Operation(summary = "Search and filter products")
    @GetMapping("/api/products")
    PageResponse<ProductSummary> search(
            @Parameter(description = "Words to find in the product or brand name") @RequestParam(required = false) String q,
            @Parameter(description = "Category slug; includes subcategories") @RequestParam(required = false) String category,
            @Parameter(description = "Brand slugs") @RequestParam(required = false) List<String> brand,
            @Parameter(description = "Rial") @RequestParam(required = false) Long minPrice,
            @Parameter(description = "Rial") @RequestParam(required = false) Long maxPrice,
            @RequestParam(defaultValue = "false") boolean inStock,
            @Parameter(description = "Only running amazing offers") @RequestParam(defaultValue = "false") boolean offers,
            @Parameter(description = "newest, cheapest, most-expensive, biggest-discount, top-rated, bestselling")
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        ProductSearch.Criteria criteria = new ProductSearch.Criteria(q, blankToNull(category), brand, minPrice,
                maxPrice, inStock, offers, ProductSort.parse(sort));
        return catalogService.search(criteria, Paging.page(page), Paging.size(size));
    }

    @Operation(summary = "Product page")
    @GetMapping("/api/products/{slug}")
    ProductDetail product(@PathVariable String slug) {
        return catalogService.product(slug);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
