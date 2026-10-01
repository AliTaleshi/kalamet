package com.kalamet.catalog.web;

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
import com.kalamet.catalog.service.AdminCatalogService;
import com.kalamet.common.web.PageResponse;
import com.kalamet.common.web.Paging;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin: catalog")
@RestController
@RequestMapping("/api/admin")
class AdminCatalogController {

    private final AdminCatalogService adminCatalog;

    AdminCatalogController(AdminCatalogService adminCatalog) {
        this.adminCatalog = adminCatalog;
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    CategoryRef createCategory(@Valid @RequestBody CategoryRequest request) {
        return adminCatalog.createCategory(request);
    }

    @PutMapping("/categories/{id}")
    CategoryRef updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return adminCatalog.updateCategory(id, request);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCategory(@PathVariable Long id) {
        adminCatalog.deleteCategory(id);
    }

    @PostMapping("/brands")
    @ResponseStatus(HttpStatus.CREATED)
    BrandResponse createBrand(@Valid @RequestBody BrandRequest request) {
        return adminCatalog.createBrand(request);
    }

    @PutMapping("/brands/{id}")
    BrandResponse updateBrand(@PathVariable Long id, @Valid @RequestBody BrandRequest request) {
        return adminCatalog.updateBrand(id, request);
    }

    @DeleteMapping("/brands/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteBrand(@PathVariable Long id) {
        adminCatalog.deleteBrand(id);
    }

    @GetMapping("/products")
    PageResponse<AdminProductSummary> products(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) Boolean active,
                                               @RequestParam(required = false) Integer page,
                                               @RequestParam(required = false) Integer size) {
        return adminCatalog.searchProducts(q, active,
                Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/products/{id}")
    AdminProductDetail product(@PathVariable Long id) {
        return adminCatalog.product(id);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    AdminProductDetail createProduct(@Valid @RequestBody ProductCreateRequest request) {
        return adminCatalog.createProduct(request);
    }

    @PutMapping("/products/{id}")
    AdminProductDetail updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return adminCatalog.updateProduct(id, request);
    }

    @PutMapping("/products/{id}/specs")
    AdminProductDetail replaceSpecs(@PathVariable Long id, @Valid @RequestBody List<@Valid SpecRequest> specs) {
        return adminCatalog.replaceSpecs(id, specs);
    }

    @PostMapping("/products/{id}/images")
    @ResponseStatus(HttpStatus.CREATED)
    AdminProductDetail addImage(@PathVariable Long id, @Valid @RequestBody ImageRequest request) {
        return adminCatalog.addImage(id, request);
    }

    @DeleteMapping("/products/{id}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
        adminCatalog.deleteImage(id, imageId);
    }

    @PostMapping("/products/{id}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    AdminVariantResponse addVariant(@PathVariable Long id, @Valid @RequestBody VariantRequest request) {
        return adminCatalog.addVariant(id, request);
    }

    @PutMapping("/variants/{id}")
    AdminVariantResponse updateVariant(@PathVariable Long id, @Valid @RequestBody VariantRequest request) {
        return adminCatalog.updateVariant(id, request);
    }

    @GetMapping("/variants/low-stock")
    List<LowStockResponse> lowStock(@RequestParam(defaultValue = "5") int threshold) {
        return adminCatalog.lowStock(threshold);
    }
}
