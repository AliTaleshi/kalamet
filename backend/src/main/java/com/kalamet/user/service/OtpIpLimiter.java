package com.kalamet.user.service;

import com.kalamet.common.error.TooManyRequestsException;
import com.kalamet.config.KalametProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Fixed-window limit on login-code requests per client IP, so one client cannot make the
 * store send SMS to thousands of numbers. Kept in memory: with several API instances each
 * one enforces its own limit (move it to Redis if that ever matters).
 */
@Component
public class OtpIpLimiter {

    private record Window(Instant start, int count) {
    }

    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxPerWindow;
    private final Duration window;
    private final Clock clock;

    OtpIpLimiter(KalametProperties properties, Clock clock) {
        this.maxPerWindow = properties.otp().maxPerIp();
        this.window = properties.otp().ipWindow();
        this.clock = clock;
    }

    public void check(String ip) {
        Instant now = clock.instant();
        Window current = windows.compute(ip, (key, existing) ->
                existing == null || !now.isBefore(existing.start().plus(window))
                        ? new Window(now, 1)
                        : new Window(existing.start(), existing.count() + 1));
        if (current.count() > maxPerWindow) {
            long wait = Duration.between(now, current.start().plus(window)).toSeconds() + 1;
            throw new TooManyRequestsException("OTP_IP_LIMIT",
                    "درخواست‌های زیادی از این شبکه ارسال شده است. لطفاً کمی بعد دوباره تلاش کنید.", wait);
        }
    }

    @Scheduled(fixedDelayString = "PT10M")
    void forgetExpiredWindows() {
        Instant now = clock.instant();
        windows.values().removeIf(w -> !now.isBefore(w.start().plus(window)));
    }
}
