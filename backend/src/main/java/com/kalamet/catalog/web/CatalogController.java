package com.kalamet.catalog.web;

import com.kalamet.catalog.dto.CatalogDtos.BrandResponse;
import com.kalamet.catalog.dto.CatalogDtos.CategoryDetail;
import com.kalamet.catalog.dto.CatalogDtos.CategoryNode;
import com.kalamet.catalog.dto.CatalogDtos.ProductDetail;
import com.kalamet.catalog.dto.CatalogDtos.ProductSummary;
import com.kalamet.catalog.dto.ProductQuery;
import com.kalamet.catalog.dto.ProductSort;
import com.kalamet.catalog.service.CatalogService;
import com.kalamet.common.web.PageResponse;
import com.kalamet.common.web.Paging;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;
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
            @Parameter(description = "Words to find in the product or brand name")
            @RequestParam(required = false) @Size(max = 200) String q,
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
        List<String> brands = brand == null ? List.of()
                : brand.stream().map(CatalogController::blankToNull).filter(Objects::nonNull).toList();
        ProductQuery query = new ProductQuery(q, blankToNull(category), brands, minPrice, maxPrice, inStock, offers,
                ProductSort.parse(sort));
        return catalogService.search(query, Paging.page(page), Paging.size(size));
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
