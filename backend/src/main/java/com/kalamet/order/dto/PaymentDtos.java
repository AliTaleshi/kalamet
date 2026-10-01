package com.kalamet.order.dto;

import com.kalamet.order.domain.Payment;
import com.kalamet.order.domain.PaymentGateway;
import com.kalamet.order.domain.PaymentStatus;
import java.time.Instant;

/** Payment bodies. Money in Rial. */
public final class PaymentDtos {

    private PaymentDtos() {
    }

    /** {@code gateway} is optional: Zarinpal when configured, otherwise the mock gateway. */
    public record PayRequest(PaymentGateway gateway) {
    }

    public record PayResponse(String orderNumber, PaymentGateway gateway, String authority, String paymentUrl) {
    }

    public record PaymentResponse(Long id, PaymentGateway gateway, PaymentStatus status, long amount, String refId,
                                  String cardPan, String failureReason, Instant paidAt, Instant createdAt) {

        public static PaymentResponse of(Payment p) {
            return new PaymentResponse(p.getId(), p.getGateway(), p.getStatus(), p.getAmount(), p.getRefId(),
                    p.getCardPan(), p.getFailureReason(), p.getPaidAt(), p.getCreatedAt());
        }
    }
}
