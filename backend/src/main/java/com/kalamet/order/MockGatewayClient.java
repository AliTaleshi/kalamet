package com.kalamet.order;

import com.kalamet.config.KalametProperties;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Pretend gateway: sends the customer to a page on this API with "pay" and "cancel"
 * buttons, then back to the normal callback. Verification always succeeds.
 */
@Component
class MockGatewayClient implements PaymentGatewayClient {

    static final String AUTHORITY_PREFIX = "MOCK-";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final boolean enabled;

    MockGatewayClient(KalametProperties properties) {
        this.enabled = properties.payment().mockEnabled();
    }

    @Override
    public PaymentGateway gateway() {
        return PaymentGateway.MOCK;
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public StartResult start(long amount, String description, String mobile, String callbackUrl) {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        String authority = AUTHORITY_PREFIX + HexFormat.of().formatHex(bytes);
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/payments/mock/{authority}")
                .buildAndExpand(authority)
                .toUriString();
        return new StartResult(authority, url);
    }

    @Override
    public VerifyResult verify(String authority, long amount) {
        String refId = String.valueOf(1_000_000_000L + (long) (RANDOM.nextDouble() * 8_999_999_999L));
        return VerifyResult.ok(refId, "603799******1234");
    }
}
