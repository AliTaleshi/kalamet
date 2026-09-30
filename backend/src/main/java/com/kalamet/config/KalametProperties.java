package com.kalamet.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Application settings under {@code kalamet.*}; defaults live in application.yml. */
@Validated
@ConfigurationProperties("kalamet")
public record KalametProperties(
        @NotBlank String frontendUrl,
        List<String> corsOrigins,
        List<String> adminMobiles,
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Otp otp,
        @Valid @NotNull Sms sms,
        @Valid @NotNull Shipping shipping,
        @Valid @NotNull Cart cart,
        @Valid @NotNull Order order,
        @Valid @NotNull Payment payment) {

    public KalametProperties {
        corsOrigins = corsOrigins == null ? List.of() : corsOrigins;
        adminMobiles = adminMobiles == null ? List.of() : adminMobiles;
    }

    /** HS256 signing key: at least 32 bytes. */
    public record Jwt(@NotBlank @Size(min = 32) String secret, @NotBlank String issuer,
                      @NotNull Duration accessTokenTtl, @NotNull Duration refreshTokenTtl) {
    }

    /**
     * Login codes. {@code hashSecret} keys the HMAC stored in otp_codes, so a leaked table
     * cannot be brute-forced offline. {@code maxPerIp} code requests are allowed per client IP
     * per {@code ipWindow}, against SMS flooding across many numbers. {@code demoMode} returns
     * the code in the API response (for a public demo without SMS); never enable it with a
     * real SMS provider.
     */
    public record Otp(@Min(4) int length, @NotNull Duration ttl, @NotNull Duration resendInterval,
                      @Min(1) int maxPerHour, @Min(1) int maxAttempts, @Min(1) int maxPerIp,
                      @NotNull Duration ipWindow, @NotBlank @Size(min = 16) String hashSecret,
                      boolean demoMode) {
    }

    public record Sms(@NotNull SmsProvider provider, @Valid @NotNull Kavenegar kavenegar) {

        public enum SmsProvider { LOG, KAVENEGAR }

        /** {@code template} is a Kavenegar "verify lookup" template with a single %token. */
        public record Kavenegar(String apiKey, @NotBlank String template, @NotBlank String baseUrl) {
        }
    }

    /** Rial amounts. Orders with items worth at least {@code freeThreshold} ship free. */
    public record Shipping(@Min(0) long flatFee, @Min(0) long freeThreshold) {
    }

    public record Cart(@Min(1) int maxQuantityPerItem) {
    }

    /** Unpaid orders are cancelled and their stock released after {@code paymentTimeout}. */
    public record Order(@NotNull Duration paymentTimeout) {
    }

    /**
     * {@code callbackUrl}: where gateways send the customer back to. Empty means "this API's
     * /api/payments/callback as the current request sees it" (proxy headers are honoured).
     */
    public record Payment(boolean mockEnabled, String callbackUrl, @Valid @NotNull Zarinpal zarinpal) {

        public record Zarinpal(String merchantId, @NotBlank String baseUrl) {

            public boolean configured() {
                return merchantId != null && !merchantId.isBlank();
            }
        }
    }
}
