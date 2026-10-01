package com.kalamet.order.dto;

import com.kalamet.order.domain.OrderStatus;
import com.kalamet.order.domain.ShippingAddress;
import com.kalamet.order.dto.PaymentDtos.PaymentResponse;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Order bodies. Money in Rial. */
public final class OrderDtos {

    private OrderDtos() {
    }

    /**
     * {@code expectedPayable}: the payable amount the customer saw in the cart (Rial). If prices
     * changed since (an offer ended, an admin edited a price), checkout is refused with
     * PRICES_CHANGED instead of charging a different amount. Optional but recommended.
     */
    public record CheckoutRequest(@NotNull(message = "نشانی تحویل را انتخاب کنید.") Long addressId,
                                  Long expectedPayable) {
    }

    public record StatusChangeRequest(@NotNull OrderStatus status) {
    }

    public record OrderLine(Long variantId, String productSlug, String productName, String sku,
                            Map<String, String> attributes, String imageUrl, long unitPrice, Long originalPrice,
                            int quantity, long lineTotal) {
    }

    public record ShippingAddressResponse(String recipientName, String recipientMobile, String province, String city,
                                          String addressLine, String plaque, String unit, String postalCode) {

        public static ShippingAddressResponse of(ShippingAddress a) {
            return new ShippingAddressResponse(a.getRecipientName(), a.getRecipientMobile(), a.getProvince(),
                    a.getCity(), a.getAddressLine(), a.getPlaque(), a.getUnit(), a.getPostalCode());
        }
    }

    public record CustomerResponse(Long id, String mobile, String name) {
    }

    /**
     * @param payableUntil while PENDING_PAYMENT: the order is cancelled if not paid by then
     * @param customer     filled in for admins only
     */
    public record OrderResponse(String orderNumber, OrderStatus status, Instant createdAt, Instant payableUntil,
                                List<OrderLine> items, long itemsTotal, long discountTotal, long shippingFee,
                                long total, ShippingAddressResponse shippingAddress, List<PaymentResponse> payments,
                                CustomerResponse customer) {
    }

    public record OrderSummary(String orderNumber, OrderStatus status, long total, int itemCount,
                               List<String> imageUrls, Instant createdAt, CustomerResponse customer) {
    }
}
