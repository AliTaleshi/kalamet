package com.kalamet.review;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.kalamet.IntegrationTest;
import org.junit.jupiter.api.Test;

class ReviewApiTest extends IntegrationTest {

    private static final String REVIEWS = "/api/products/voltra-usb-c-charger-65w/reviews";

    @Test
    void oneReviewPerProductAndEditsGoBackToModeration() throws Exception {
        Session customer = signInCustomer();
        String created = body(postAs(REVIEWS, customer, "{\"rating\": 5}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verifiedPurchase").value(false)));
        long id = ((Number) JsonPath.read(created, "$.id")).longValue();

        postAs(REVIEWS, customer, "{\"rating\": 3}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("REVIEW_EXISTS"));

        patchAs("/api/admin/reviews/" + id, signInAdmin(), "{\"status\": \"APPROVED\"}").andExpect(status().isOk());
        putAs("/api/me/reviews/" + id, customer, "{\"rating\": 2, \"comment\": \"بعد از یک ماه داغ می‌کند.\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
        getAs("/api/me/reviews", customer).andExpect(jsonPath("$.items[0].product.slug").value("voltra-usb-c-charger-65w"));

        deleteAs("/api/me/reviews/" + id, signInCustomer()).andExpect(status().isNotFound());
        deleteAs("/api/me/reviews/" + id, customer).andExpect(status().isNoContent());
    }

    @Test
    void ratingMustBeOneToFive() throws Exception {
        postAs(REVIEWS, signInCustomer(), "{\"rating\": 6}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.rating").value("امتیاز باید بین ۱ تا ۵ باشد."));
    }

    @Test
    void adminSeesThePendingQueue() throws Exception {
        postAs("/api/products/koozegar-stoneware-mug/reviews", signInCustomer(), "{\"rating\": 4, \"title\": \"قشنگ\"}")
                .andExpect(status().isCreated());
        getAs("/api/admin/reviews?status=PENDING", signInAdmin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.title == 'قشنگ')].product.slug").value("koozegar-stoneware-mug"));
    }
}
