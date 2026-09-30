package com.kalamet.catalog;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.kalamet.IntegrationTest;
import org.junit.jupiter.api.Test;

class AdminCatalogApiTest extends IntegrationTest {

    @Test
    void adminCreatesAProductThatShowsUpInTheStore() throws Exception {
        Session admin = signInAdmin();
        long categoryId = jdbc.sql("SELECT id FROM categories WHERE slug = 'mugs'").query(Long.class).single();

        String created = body(postAs("/api/admin/products", admin, """
                {"product": {"categoryId": %d, "name": "ماگ لعابی \u0643وچ\u0643", "slug": "glazed-mini-mug"},
                 "variants": [
                   {"sku": "GLZ-BLU", "attributes": {"color": "آبی"}, "price": 3500000, "stock": 5},
                   {"sku": "GLZ-RED", "attributes": {"color": "قرمز"}, "price": 3500000,
                    "compareAtPrice": 4000000, "stock": 0}],
                 "specs": [{"name": "حجم", "value": "۲۰۰ میلی‌لیتر"}],
                 "images": [{"url": "https://picsum.photos/seed/glz/800/800", "sortOrder": 0}]}
                """.formatted(categoryId)).andExpect(status().isCreated()));
        // The Arabic Kaf in the name was stored as Persian Kaf.
        org.assertj.core.api.Assertions.assertThat((String) JsonPath.read(created, "$.name")).isEqualTo("ماگ لعابی کوچک");

        getAs("/api/products/glazed-mini-mug", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variants[?(@.sku == 'GLZ-RED')].inStock").value(false))
                .andExpect(jsonPath("$.variants[?(@.sku == 'GLZ-BLU')].remaining").value(5));
        getAs("/api/products?category=home-kitchen&q=لعابی", null)
                .andExpect(jsonPath("$.items[0].slug").value("glazed-mini-mug"))
                .andExpect(jsonPath("$.items[0].price").value(3500000))
                .andExpect(jsonPath("$.items[0].inStock").value(true));

        // Deactivated products disappear from the store but stay in the admin list.
        long productId = ((Number) JsonPath.read(created, "$.id")).longValue();
        putAs("/api/admin/products/" + productId, admin, """
                {"categoryId": %d, "name": "ماگ لعابی کوچک", "slug": "glazed-mini-mug", "active": false}
                """.formatted(categoryId)).andExpect(status().isOk());
        getAs("/api/products/glazed-mini-mug", null).andExpect(status().isNotFound());
        getAs("/api/admin/products?q=glazed", admin).andExpect(jsonPath("$.items[0].active").value(false));
    }

    @Test
    void duplicatesAndBrokenPricesGetClearMessages() throws Exception {
        Session admin = signInAdmin();
        long categoryId = jdbc.sql("SELECT id FROM categories WHERE slug = 'mugs'").query(Long.class).single();
        String product = """
                {"product": {"categoryId": %d, "name": "ماگ", "slug": "%s"},
                 "variants": [{"sku": "%s", "price": 1000, %s "stock": 1}]}
                """;

        postAs("/api/admin/products", admin, product.formatted(categoryId, "koozegar-stoneware-mug", "NEW-SKU-1", ""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("کالای دیگری با این نامک وجود دارد."));
        postAs("/api/admin/products", admin, product.formatted(categoryId, "new-mug-1", "KZG-MUG-CRM", ""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("تنوع دیگری با این کد انبار (SKU) وجود دارد."));
        postAs("/api/admin/products", admin, product.formatted(categoryId, "new-mug-2", "NEW-SKU-2",
                "\"compareAtPrice\": 900,"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COMPARE_AT_PRICE"));
        postAs("/api/admin/products", admin, product.formatted(categoryId, "Bad Slug", "NEW-SKU-3", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void staleVariantEditsAreRejected() throws Exception {
        Session admin = signInAdmin();
        long categoryId = jdbc.sql("SELECT id FROM categories WHERE slug = 'mugs'").query(Long.class).single();
        String created = body(postAs("/api/admin/products", admin, """
                {"product": {"categoryId": %d, "name": "ماگ نسخه‌دار", "slug": "versioned-mug"},
                 "variants": [{"sku": "VER-MUG", "price": 1000000, "stock": 10}]}
                """.formatted(categoryId)).andExpect(status().isCreated()));
        long variantId = ((Number) JsonPath.read(created, "$.variants[0].id")).longValue();
        long version = ((Number) JsonPath.read(created, "$.variants[0].version")).longValue();

        // A sale happens after the admin opened the form.
        jdbc.sql("UPDATE product_variants SET stock_quantity = 9, version = version + 1 WHERE id = :id")
                .param("id", variantId).update();

        String edit = """
                {"sku": "VER-MUG", "price": 1000000, "stock": 15, "version": %d}
                """;
        putAs("/api/admin/variants/" + variantId, admin, edit.formatted(version))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_VARIANT"));
        putAs("/api/admin/variants/" + variantId, admin, edit.formatted(version + 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(15));
    }

    @Test
    void categoriesInUseCannotBeDeleted() throws Exception {
        Session admin = signInAdmin();
        long fashion = jdbc.sql("SELECT id FROM categories WHERE slug = 'fashion'").query(Long.class).single();
        long tShirts = jdbc.sql("SELECT id FROM categories WHERE slug = 't-shirts'").query(Long.class).single();

        deleteAs("/api/admin/categories/" + fashion, admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_HAS_CHILDREN"));
        deleteAs("/api/admin/categories/" + tShirts, admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_HAS_PRODUCTS"));
        // A category cannot be moved under its own child.
        putAs("/api/admin/categories/" + fashion, admin, """
                {"parentId": %d, "name": "مد و پوشاک", "slug": "fashion"}
                """.formatted(tShirts)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CATEGORY_CYCLE"));
    }
}
