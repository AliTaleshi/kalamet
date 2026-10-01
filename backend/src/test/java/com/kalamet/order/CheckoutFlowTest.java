package com.kalamet.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.kalamet.IntegrationTest;
import com.kalamet.order.service.OrderService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CheckoutFlowTest extends IntegrationTest {

    @Autowired
    private OrderService orderService;

    @Test
    void fromCartToDeliveredOrderWithVerifiedReview() throws Exception {
        Session customer = signInCustomer();
        long addressId = createAddress(customer);
        long tee = variantId("ARN-TEE-BLK-M");
        int stockBefore = stock("ARN-TEE-BLK-M");

        // Cart: 2 x 12,900,000 (was 15,900,000) = 25,800,000 Rial, above the free-shipping threshold.
        postAs("/api/cart/items", customer, "{\"variantId\": %d, \"quantity\": 1}".formatted(tee)).andExpect(status().isOk());
        postAs("/api/cart/items", customer, "{\"variantId\": %d, \"quantity\": 1}".formatted(tee))
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andExpect(jsonPath("$.lines[0].quantity").value(2))
                .andExpect(jsonPath("$.itemsTotal").value(25_800_000))
                .andExpect(jsonPath("$.discountTotal").value(6_000_000))
                .andExpect(jsonPath("$.shippingFee").value(0))
                .andExpect(jsonPath("$.payable").value(25_800_000));

        // Checkout reserves stock and empties the cart.
        String order = body(postAs("/api/orders", customer, "{\"addressId\": %d}".formatted(addressId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.total").value(25_800_000))
                .andExpect(jsonPath("$.items[0].originalPrice").value(15_900_000))
                .andExpect(jsonPath("$.shippingAddress.postalCode").value("1234567890"))
                .andExpect(jsonPath("$.payableUntil").exists()));
        String orderNumber = JsonPath.read(order, "$.orderNumber");
        assertThat(stock("ARN-TEE-BLK-M")).isEqualTo(stockBefore - 2);
        getAs("/api/cart", customer).andExpect(jsonPath("$.lines.length()").value(0));

        // Pay with the mock gateway.
        String authority = startMockPayment(customer, orderNumber);
        getAs("/api/payments/mock/" + authority, null)
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(containsString(orderNumber)));
        getAs("/api/payments/callback?Authority=" + authority + "&Status=OK", null)
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        startsWith("http://localhost:3000/checkout/result?status=success&order=" + orderNumber + "&ref=")));
        // A repeated callback changes nothing.
        getAs("/api/payments/callback?Authority=" + authority + "&Status=OK", null)
                .andExpect(header().string("Location", containsString("status=success")));

        getAs("/api/orders/" + orderNumber, customer)
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.payments.length()").value(1))
                .andExpect(jsonPath("$.payments[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.payments[0].refId").exists())
                .andExpect(jsonPath("$.payableUntil").doesNotExist());
        postAs("/api/orders/" + orderNumber + "/cancel", customer, "").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_CANCELLABLE"));

        // Fulfilment by an admin; shipping does not touch stock.
        Session admin = signInAdmin();
        patchAs("/api/admin/orders/" + orderNumber + "/status", admin, "{\"status\": \"DELIVERED\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATUS_CHANGE"));
        patchAs("/api/admin/orders/" + orderNumber + "/status", admin, "{\"status\": \"SHIPPED\"}")
                .andExpect(status().isOk());
        patchAs("/api/admin/orders/" + orderNumber + "/status", admin, "{\"status\": \"DELIVERED\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.id").exists());
        assertThat(stock("ARN-TEE-BLK-M")).isEqualTo(stockBefore - 2);

        // The review is marked as a verified purchase and waits for moderation.
        String review = body(postAs("/api/products/arian-essential-cotton-tee/reviews", customer, """
                {"rating": 4, "title": "خوش‌دوخت", "comment": "جنس پارچه خوب است.", "recommended": true}
                """).andExpect(status().isCreated())
                .andExpect(jsonPath("$.verifiedPurchase").value(true))
                .andExpect(jsonPath("$.status").value("PENDING")));
        long reviewId = ((Number) JsonPath.read(review, "$.id")).longValue();
        String visible = "$.items[?(@.id == " + reviewId + ")]";
        getAs("/api/products/arian-essential-cotton-tee/reviews", null)
                .andExpect(jsonPath(visible).isEmpty());

        patchAs("/api/admin/reviews/" + reviewId, admin, "{\"status\": \"APPROVED\"}").andExpect(status().isOk());
        getAs("/api/products/arian-essential-cotton-tee/reviews", null)
                .andExpect(jsonPath(visible + ".authorName").value("کاربر کالامت"));
        getAs("/api/products?q=arian&sort=top-rated", null)
                .andExpect(jsonPath("$.items[0].slug").value("arian-essential-cotton-tee"))
                .andExpect(jsonPath("$.items[0].reviewCount").value(1));
    }

    @Test
    void checkoutRefusesWhenThePriceChangedSinceTheCart() throws Exception {
        Session customer = signInCustomer();
        long addressId = createAddress(customer);
        postAs("/api/cart/items", customer, "{\"variantId\": %d, \"quantity\": 1}".formatted(variantId("VLT-65W")))
                .andExpect(jsonPath("$.payable").value(18_500_000));

        postAs("/api/orders", customer, "{\"addressId\": %d, \"expectedPayable\": 17000000}".formatted(addressId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRICES_CHANGED"));
        getAs("/api/cart", customer).andExpect(jsonPath("$.lines.length()").value(1));   // nothing was ordered

        postAs("/api/orders", customer, "{\"addressId\": %d, \"expectedPayable\": 18500000}".formatted(addressId))
                .andExpect(status().isCreated());
    }

    @Test
    void freeOrdersAreMarkedPaidAtCheckout() throws Exception {
        Session admin = signInAdmin();
        long categoryId = jdbc.sql("SELECT id FROM categories WHERE slug = 'mugs'").query(Long.class).single();
        postAs("/api/admin/products", admin, """
                {"product": {"categoryId": %d, "name": "ماگ هدیه", "slug": "gift-mug"},
                 "variants": [{"sku": "GIFT-MUG", "price": 0, "stock": 5}]}
                """.formatted(categoryId)).andExpect(status().isCreated());

        Session customer = signInCustomer();
        long addressId = createAddress(customer);
        postAs("/api/cart/items", customer, "{\"variantId\": %d, \"quantity\": 1}".formatted(variantId("GIFT-MUG")))
                .andExpect(jsonPath("$.payable").value(0));
        String orderNumber = JsonPath.read(body(postAs("/api/orders", customer, "{\"addressId\": %d}".formatted(addressId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.total").value(0))), "$.orderNumber");
        postAs("/api/orders/" + orderNumber + "/pay", customer, "{}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_PAYABLE"));
    }

    @Test
    void adminFindsOrdersByMobileInAnyFormat() throws Exception {
        String mobile = newMobile();
        Session customer = signIn(mobile);
        long addressId = createAddress(customer);
        postAs("/api/cart/items", customer, "{\"variantId\": %d, \"quantity\": 1}".formatted(variantId("KZG-MUG-GRY")));
        String orderNumber = JsonPath.read(body(postAs("/api/orders", customer,
                "{\"addressId\": %d}".formatted(addressId))), "$.orderNumber");

        StringBuilder persian = new StringBuilder("+۹۸");
        mobile.substring(1).chars().forEach(c -> persian.append((char) ('۰' + (c - '0'))));
        getAs("/api/admin/orders?q=" + persian, signInAdmin())
                .andExpect(jsonPath("$.items[0].orderNumber").value(orderNumber));
        getAs("/api/admin/orders?q=" + orderNumber.toLowerCase(), signInAdmin())
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void smallOrdersPayShipping() throws Exception {
        Session customer = signInCustomer();
        postAs("/api/cart/items", customer, "{\"variantId\": %d, \"quantity\": 1}".formatted(variantId("VLT-65W")))
                .andExpect(jsonPath("$.itemsTotal").value(18_500_000))
                .andExpect(jsonPath("$.shippingFee").value(0));
        deleteAs("/api/cart/items/" + variantId("VLT-65W"), customer).andExpect(jsonPath("$.itemCount").value(0));

        long mug = variantId("KZG-MUG-GRY");
        postAs("/api/cart/items", customer, "{\"variantId\": %d, \"quantity\": 1}".formatted(mug))
                .andExpect(jsonPath("$.itemsTotal").value(4_800_000))
                .andExpect(jsonPath("$.shippingFee").value(600_000))
                .andExpect(jsonPath("$.freeShippingRemaining").value(5_200_000))
                .andExpect(jsonPath("$.payable").value(5_400_000));
    }

    @Test
    void quantitiesAreLimited() throws Exception {
        Session customer = signInCustomer();
        long hoodie = variantId("ARN-HOD-NVY-L");   // 12 in stock, limit 10 per line
        postAs("/api/cart/items", customer, "{\"variantId\": %d, \"quantity\": 11}".formatted(hoodie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUANTITY_LIMIT"))
                .andExpect(jsonPath("$.detail").value("حداکثر ۱۰ عدد از این کالا را می‌توانید سفارش دهید."));

        // Merging a guest cart caps instead of failing.
        postAs("/api/cart/merge", customer, "{\"items\": [{\"variantId\": %d, \"quantity\": 50}]}".formatted(hoodie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].quantity").value(10))
                .andExpect(jsonPath("$.lines[0].maxQuantity").value(10));
    }

    @Test
    void failedPaymentCanBeRetriedAndCancellingReleasesStock() throws Exception {
        Session customer = signInCustomer();
        long addressId = createAddress(customer);
        int stockBefore = stock("KZG-MUG-CRM");
        postAs("/api/cart/items", customer, "{\"variantId\": %d, \"quantity\": 3}".formatted(variantId("KZG-MUG-CRM")));
        String orderNumber = JsonPath.read(body(postAs("/api/orders", customer,
                "{\"addressId\": %d}".formatted(addressId))), "$.orderNumber");
        assertThat(stock("KZG-MUG-CRM")).isEqualTo(stockBefore - 3);

        String authority = startMockPayment(customer, orderNumber);
        getAs("/api/payments/callback?Authority=" + authority + "&Status=NOK", null)
                .andExpect(header().string("Location", containsString("status=failed")));
        getAs("/api/orders/" + orderNumber, customer)
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.payments[0].status").value("FAILED"));

        startMockPayment(customer, orderNumber);   // a second attempt is allowed
        postAs("/api/orders/" + orderNumber + "/cancel", customer, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.payments[0].status").value("FAILED"));
        assertThat(stock("KZG-MUG-CRM")).isEqualTo(stockBefore);
    }

    @Test
    void unpaidOrdersExpire() throws Exception {
        Session customer = signInCustomer();
        long addressId = createAddress(customer);
        int stockBefore = stock("AS700-SLV");
        postAs("/api/cart/items", customer, "{\"variantId\": %d, \"quantity\": 1}".formatted(variantId("AS700-SLV")));
        String orderNumber = JsonPath.read(body(postAs("/api/orders", customer,
                "{\"addressId\": %d}".formatted(addressId))), "$.orderNumber");

        jdbc.sql("UPDATE orders SET created_at = now() - interval '1 hour' WHERE order_number = :n")
                .param("n", orderNumber).update();
        for (Long id : orderService.expiredOrderIds()) {
            orderService.expire(id);
        }

        getAs("/api/orders/" + orderNumber, customer).andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(stock("AS700-SLV")).isEqualTo(stockBefore);
        postAs("/api/orders/" + orderNumber + "/pay", customer, "{\"gateway\": \"MOCK\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void checkoutNeedsAnAvailableCart() throws Exception {
        Session customer = signInCustomer();
        long addressId = createAddress(customer);
        postAs("/api/orders", customer, "{\"addressId\": %d}".formatted(addressId))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CART_EMPTY"));

        long charger = variantId("VLT-65W");
        postAs("/api/cart/items", customer, "{\"variantId\": %d, \"quantity\": 1}".formatted(charger));
        jdbc.sql("UPDATE product_variants SET active = false WHERE id = :id").param("id", charger).update();
        try {
            getAs("/api/cart", customer)
                    .andExpect(jsonPath("$.hasIssues").value(true))
                    .andExpect(jsonPath("$.lines[0].issue").value("UNAVAILABLE"))
                    .andExpect(jsonPath("$.itemsTotal").value(0));
            postAs("/api/orders", customer, "{\"addressId\": %d}".formatted(addressId))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CART_HAS_ISSUES"));
        } finally {
            jdbc.sql("UPDATE product_variants SET active = true WHERE id = :id").param("id", charger).update();
        }
    }

    @Test
    void endedOfferIsChargedAtTheRegularPrice() throws Exception {
        Session admin = signInAdmin();
        long categoryId = jdbc.sql("SELECT id FROM categories WHERE slug = 'mugs'").query(Long.class).single();
        postAs("/api/admin/products", admin, """
                {"product": {"categoryId": %d, "name": "ماگ حراجی", "slug": "flash-sale-mug"},
                 "variants": [{"sku": "FLASH-MUG", "price": 1000000, "compareAtPrice": 1500000,
                               "discountEndsAt": "2020-01-01T00:00:00Z", "stock": 5}]}
                """.formatted(categoryId)).andExpect(status().isCreated());

        Session customer = signInCustomer();
        postAs("/api/cart/items", customer, "{\"variantId\": %d, \"quantity\": 1}".formatted(variantId("FLASH-MUG")))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(1_500_000))
                .andExpect(jsonPath("$.lines[0].originalPrice").doesNotExist())
                .andExpect(jsonPath("$.discountTotal").value(0));
        getAs("/api/products?q=حراجی", null)
                .andExpect(jsonPath("$.items[0].price").value(1_500_000))
                .andExpect(jsonPath("$.items[0].discountPercent").value(0));
    }

    @Test
    void onlyOneCustomerGetsTheLastUnit() throws Exception {
        Session admin = signInAdmin();
        long categoryId = jdbc.sql("SELECT id FROM categories WHERE slug = 'mugs'").query(Long.class).single();
        postAs("/api/admin/products", admin, """
                {"product": {"categoryId": %d, "name": "ماگ تک", "slug": "last-unit-mug"},
                 "variants": [{"sku": "LAST-UNIT", "price": 2000000, "stock": 1}]}
                """.formatted(categoryId)).andExpect(status().isCreated());
        long variant = variantId("LAST-UNIT");

        List<Callable<Integer>> checkouts = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Session customer = signInCustomer();
            long addressId = createAddress(customer);
            postAs("/api/cart/items", customer, "{\"variantId\": %d, \"quantity\": 1}".formatted(variant))
                    .andExpect(status().isOk());
            checkouts.add(() -> postAs("/api/orders", customer, "{\"addressId\": %d}".formatted(addressId))
                    .andReturn().getResponse().getStatus());
        }
        ExecutorService pool = Executors.newFixedThreadPool(checkouts.size());
        List<Integer> statuses = new ArrayList<>();
        try {
            for (Future<Integer> result : pool.invokeAll(checkouts)) {
                statuses.add(result.get());
            }
        } finally {
            pool.shutdown();
        }

        assertThat(statuses).containsOnlyOnce(201);
        assertThat(statuses).filteredOn(s -> s != 201).allMatch(s -> s == 409);
        assertThat(stock("LAST-UNIT")).isZero();
    }

    @Test
    void crossingCheckoutsNeverDeadlock() throws Exception {
        Session admin = signInAdmin();
        long categoryId = jdbc.sql("SELECT id FROM categories WHERE slug = 'mugs'").query(Long.class).single();
        postAs("/api/admin/products", admin, """
                {"product": {"categoryId": %d, "name": "ماگ دوقلو", "slug": "twin-mug"},
                 "variants": [{"sku": "TWIN-A", "attributes": {"color": "سبز"}, "price": 1000000, "stock": 1000},
                              {"sku": "TWIN-B", "attributes": {"color": "زرد"}, "price": 1000000, "stock": 1000}]}
                """.formatted(categoryId)).andExpect(status().isCreated());
        long a = variantId("TWIN-A");
        long b = variantId("TWIN-B");

        List<Integer> statuses = new ArrayList<>();
        for (int round = 0; round < 5; round++) {
            List<Callable<Integer>> checkouts = new ArrayList<>();
            for (long[] order : new long[][] {{a, b}, {b, a}, {a, b}, {b, a}}) {
                Session customer = signInCustomer();
                long addressId = createAddress(customer);
                for (long variant : order) {
                    postAs("/api/cart/items", customer, "{\"variantId\": %d, \"quantity\": 1}".formatted(variant));
                }
                checkouts.add(() -> postAs("/api/orders", customer, "{\"addressId\": %d}".formatted(addressId))
                        .andReturn().getResponse().getStatus());
            }
            ExecutorService pool = Executors.newFixedThreadPool(checkouts.size());
            try {
                for (Future<Integer> result : pool.invokeAll(checkouts)) {
                    statuses.add(result.get());
                }
            } finally {
                pool.shutdown();
            }
        }
        // Losing a race must be a 409 the customer can retry, never a 500. (Deadlocks are prevented by
        // hibernate.order_updates and mapped to 409 if they happen; this test cannot force one.)
        assertThat(statuses).allMatch(s -> s == 201 || s == 409).contains(201);
    }

    private String startMockPayment(Session customer, String orderNumber) throws Exception {
        String response = body(postAs("/api/orders/" + orderNumber + "/pay", customer, "{\"gateway\": \"MOCK\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gateway").value("MOCK"))
                .andExpect(jsonPath("$.paymentUrl", containsString("/api/payments/mock/MOCK-"))));
        return JsonPath.read(response, "$.authority");
    }
}
