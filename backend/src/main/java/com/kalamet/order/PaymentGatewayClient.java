package com.kalamet.order;

/** One implementation per {@link PaymentGateway}. */
interface PaymentGatewayClient {

    PaymentGateway gateway();

    boolean enabled();

    /** Registers the payment with the gateway; the customer is then sent to {@code paymentUrl}. */
    StartResult start(long amount, String description, String mobile, String callbackUrl);

    /**
     * Confirms the payment after the customer returns. Until this succeeds the money is not
     * settled, and gateways reverse unverified payments on their own.
     */
    VerifyResult verify(String authority, long amount);

    record StartResult(String authority, String paymentUrl) {
    }

    record VerifyResult(boolean success, String refId, String cardPan, String failureReason) {

        static VerifyResult ok(String refId, String cardPan) {
            return new VerifyResult(true, refId, cardPan, null);
        }

        static VerifyResult failed(String reason) {
            return new VerifyResult(false, null, null, reason);
        }
    }
}
