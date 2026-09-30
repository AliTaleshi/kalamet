package com.kalamet.user;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Nightly cleanup of used-up login codes and expired refresh tokens. */
@Slf4j
@Component
@ConditionalOnProperty(name = "kalamet.jobs.enabled", havingValue = "true", matchIfMissing = true)
class UserMaintenanceJob {

    private static final Duration KEEP = Duration.ofDays(1);

    private final OtpService otpService;
    private final TokenService tokenService;
    private final Clock clock;

    UserMaintenanceJob(OtpService otpService, TokenService tokenService, Clock clock) {
        this.otpService = otpService;
        this.tokenService = tokenService;
        this.clock = clock;
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Tehran")
    void cleanUp() {
        Instant before = clock.instant().minus(KEEP);
        int codes = otpService.deleteOlderThan(before);
        int tokens = tokenService.deleteExpiredBefore(before);
        log.info("Cleanup removed {} login codes and {} refresh tokens", codes, tokens);
    }
}
