package com.kalamet.catalog;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kalamet.IntegrationTest;
import org.junit.jupiter.api.Test;

/** Storefront reads against the demo catalog (db/seed/R__demo_catalog.sql). */
class CatalogApiTest extends IntegrationTest {

    @Test
    void categoryTreeNestsSubcategories() throws Exception {
        getAs("/api/categories", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("digital"))
                .andExpect(jsonPath("$[0].children[*].slug", contains("headphones", "chargers")));
        getAs("/api/categories/t-shirts", null)
                .andExpect(jsonPath("$.breadcrumbs[*].slug", contains("fashion", "t-shirts")));
    }

    @Test
    void searchMatchesPersianWordsRegardlessOfHalfSpaces() throws Exception {
        // The product is named "تی‌شرت" (with a zero-width non-joiner).
        for (String query : new String[] {"تی شرت", "تیشرت", "تی‌شرت نخی", "arian"}) {
            getAs("/api/products?q=" + query, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[*].slug", hasItem("arian-essential-cotton-tee")));
        }
        getAs("/api/products?q=ت\u064Aشرت", null)   // typed with Arabic Yeh
                .andExpect(jsonPath("$.items[*].slug", hasItem("arian-essential-cotton-tee")));
    }

    @Test
    void searchIgnoresTheDigitScript() throws Exception {
        // The charger is named "... ۶۵ وات" with Persian digits.
        for (String query : new String[] {"65 وات", "۶۵ وات", "٦٥"}) {
            getAs("/api/products?q=" + query, null)
                    .andExpect(jsonPath("$.items[*].slug", hasItem("voltra-usb-c-charger-65w")));
        }
    }

    @Test
    void blankFiltersAreIgnoredAndOversizedQueriesRejected() throws Exception {
        getAs("/api/products?brand=&category=fashion", null)
                .andExpect(jsonPath("$.items[*].slug", hasItem("arian-essential-cotton-tee")));
        getAs("/api/products?q=" + "a".repeat(201), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("درخواست نامعتبر است."));
    }

    @Test
    void frameworkErrorsArePersianToo() throws Exception {
        getAs("/api/admin/orders?status=LOST", signInAdmin())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.detail").value("مقدار پارامتر «status» معتبر نیست."));
        getAs("/api/no-such-endpoint", signInAdmin())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("آدرس درخواست‌شده وجود ندارد."));
    }

    @Test
    void categoryFilterIncludesSubcategories() throws Exception {
        getAs("/api/products?category=fashion", null)
                .andExpect(jsonPath("$.items[*].slug", hasItem("arian-essential-cotton-tee")))
                .andExpect(jsonPath("$.items[*].slug", hasItem("arian-everyday-zip-hoodie")))
                .andExpect(jsonPath("$.items[*].slug", not(hasItem("koozegar-stoneware-mug"))));
    }

    @Test
    void offersFilterAndPriceSort() throws Exception {
        getAs("/api/products?offers=true", null)
                .andExpect(jsonPath("$.items[*].slug", hasItem("wireless-headphones-as-700")))
                .andExpect(jsonPath("$.items[*].offerEndsAt", everyItem(not((Object) null))));
        getAs("/api/products?category=digital&sort=most-expensive", null)
                .andExpect(jsonPath("$.items[0].slug").value("wireless-headphones-as-700"))
                .andExpect(jsonPath("$.items[0].price").value(89_000_000))
                .andExpect(jsonPath("$.items[0].originalPrice").value(104_000_000))
                .andExpect(jsonPath("$.items[0].discountPercent").value(14));
        getAs("/api/products?sort=sideways", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT"));
    }

    @Test
    void productPageGroupsOptionsAndSpecs() throws Exception {
        getAs("/api/products/arian-essential-cotton-tee", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brand.slug").value("arian"))
                .andExpect(jsonPath("$.breadcrumbs[*].slug", contains("fashion", "t-shirts")))
                .andExpect(jsonPath("$.options[0].key").value("color"))
                .andExpect(jsonPath("$.options[1].key").value("size"))
                .andExpect(jsonPath("$.options[1].values", contains("S", "M", "L")))
                .andExpect(jsonPath("$.variants.length()").value(6))
                .andExpect(jsonPath("$.specGroups[0].name").value("مشخصات کلی"))
                .andExpect(jsonPath("$.rating.distribution.5").value(0));
    }

    @Test
    void unknownProductIs404() throws Exception {
        getAs("/api/products/does-not-exist", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("کالا پیدا نشد."));
    }

    @Test
    void brandsCanBeLimitedToACategory() throws Exception {
        getAs("/api/brands?category=fashion", null)
                .andExpect(jsonPath("$[*].slug", contains("arian")));
    }
}
