package com.kalamet.user.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kalamet.common.error.TooManyRequestsException;
import com.kalamet.config.KalametProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class OtpIpLimiterTest {

    private Instant now = Instant.parse("2026-10-01T10:00:00Z");

    private final Clock clock = new Clock() {
        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    };

    private final OtpIpLimiter limiter = new OtpIpLimiter(properties(3, Duration.ofMinutes(10)), clock);

    @Test
    void limitsEachIpSeparatelyWithinTheWindow() {
        for (int i = 0; i < 3; i++) {
            limiter.check("10.0.0.1");
        }
        assertThatThrownBy(() -> limiter.check("10.0.0.1"))
                .isInstanceOf(TooManyRequestsException.class)
                .hasFieldOrPropertyWithValue("code", "OTP_IP_LIMIT");
        assertThatCode(() -> limiter.check("10.0.0.2")).doesNotThrowAnyException();
    }

    @Test
    void windowResets() {
        for (int i = 0; i < 3; i++) {
            limiter.check("10.0.0.1");
        }
        now = now.plus(Duration.ofMinutes(10));
        assertThatCode(() -> limiter.check("10.0.0.1")).doesNotThrowAnyException();
    }

    private static KalametProperties properties(int maxPerIp, Duration window) {
        KalametProperties.Otp otp = new KalametProperties.Otp(6, Duration.ofMinutes(2), Duration.ofMinutes(2), 5, 5,
                maxPerIp, window, "0123456789abcdef", false);
        return new KalametProperties("http://localhost:3000", List.of(), List.of(), null, otp, null, null, null,
                null, null);
    }
}
