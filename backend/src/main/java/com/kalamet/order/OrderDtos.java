package com.kalamet.order;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Order and payment bodies. Money in Rial. */
public final class OrderDtos {

    private OrderDtos() {
    }

    public record CheckoutRequest(@NotNull(message = "نشانی تحویل را انتخاب کنید.") Long addressId) {
    }

    /** {@code gateway} is optional: Zarinpal when configured, otherwise the mock gateway. */
    public record PayRequest(PaymentGateway gateway) {
    }

    public record PayResponse(String orderNumber, PaymentGateway gateway, String authority, String paymentUrl) {
    }

    public record StatusChangeRequest(@NotNull OrderStatus status) {
    }

    public record OrderLine(Long variantId, String productSlug, String productName, String sku,
                            Map<String, String> attributes, String imageUrl, long unitPrice, Long originalPrice,
                            int quantity, long lineTotal) {
    }

    public record ShippingAddressResponse(String recipientName, String recipientMobile, String province, String city,
                                          String addressLine, String plaque, String unit, String postalCode) {

        static ShippingAddressResponse of(ShippingAddress a) {
            return new ShippingAddressResponse(a.getRecipientName(), a.getRecipientMobile(), a.getProvince(),
                    a.getCity(), a.getAddressLine(), a.getPlaque(), a.getUnit(), a.getPostalCode());
        }
    }

    public record PaymentResponse(Long id, PaymentGateway gateway, PaymentStatus status, long amount, String refId,
                                  String cardPan, String failureReason, Instant paidAt, Instant createdAt) {

        static PaymentResponse of(Payment p) {
            return new PaymentResponse(p.getId(), p.getGateway(), p.getStatus(), p.getAmount(), p.getRefId(),
                    p.getCardPan(), p.getFailureReason(), p.getPaidAt(), p.getCreatedAt());
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
