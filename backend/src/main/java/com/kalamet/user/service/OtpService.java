package com.kalamet.user.service;

import com.kalamet.common.error.ApiException;
import com.kalamet.common.error.TooManyRequestsException;
import com.kalamet.common.text.PersianText;
import com.kalamet.config.KalametProperties;
import com.kalamet.user.domain.OtpCode;
import com.kalamet.user.repository.OtpCodeRepository;
import com.kalamet.user.sms.SmsSender;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues and checks one-time login codes. Limits: one code per {@code resendInterval}, at
 * most {@code maxPerHour} codes per number per hour, {@code maxAttempts} wrong guesses per code.
 */
@Service
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpCodeRepository otpCodes;
    private final JdbcClient jdbc;
    private final SmsSender smsSender;
    private final KalametProperties.Otp settings;
    private final Clock clock;

    OtpService(OtpCodeRepository otpCodes, JdbcClient jdbc, SmsSender smsSender, KalametProperties properties,
               Clock clock) {
        this.otpCodes = otpCodes;
        this.jdbc = jdbc;
        this.smsSender = smsSender;
        this.settings = properties.otp();
        this.clock = clock;
    }

    public record IssuedCode(long expiresInSeconds, long resendInSeconds, String code) {
    }

    @Transactional
    public IssuedCode issue(String mobile) {
        // Serialises requests for the same number, so parallel calls cannot both pass the limits.
        jdbc.sql("SELECT 1 FROM pg_advisory_xact_lock(hashtext(:mobile))").param("mobile", mobile)
                .query(Integer.class).single();
        Instant now = clock.instant();
        otpCodes.findFirstByMobileOrderByCreatedAtDesc(mobile).ifPresent(last -> {
            Instant resendAt = last.getCreatedAt().plus(settings.resendInterval());
            if (now.isBefore(resendAt)) {
                long wait = Duration.between(now, resendAt).toSeconds() + 1;
                throw new TooManyRequestsException("OTP_RESEND_TOO_SOON",
                        "کد قبلی هنوز معتبر است. پس از " + PersianText.digits(wait) + " ثانیه دوباره تلاش کنید.", wait);
            }
        });
        if (otpCodes.countByMobileAndCreatedAtAfter(mobile, now.minus(Duration.ofHours(1))) >= settings.maxPerHour()) {
            throw new TooManyRequestsException("OTP_HOURLY_LIMIT",
                    "تعداد درخواست کد بیش از حد مجاز است. لطفاً یک ساعت دیگر تلاش کنید.", 3600);
        }

        String code = randomCode();
        otpCodes.save(new OtpCode(mobile, hash(code), now, now.plus(settings.ttl())));
        smsSender.sendLoginCode(mobile, code);
        return new IssuedCode(settings.ttl().toSeconds(), settings.resendInterval().toSeconds(), code);
    }

    /**
     * Checks a code against the latest one issued to the number. Wrong guesses are counted
     * even though an exception is thrown, hence {@code noRollbackFor}.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public void verify(String mobile, String code) {
        Instant now = clock.instant();
        OtpCode otp = otpCodes.findFirstWithLockByMobileOrderByCreatedAtDesc(mobile)
                .filter(candidate -> candidate.usable(now))
                .orElseThrow(() -> ApiException.badRequest("OTP_EXPIRED",
                        "کد تأیید منقضی شده است. لطفاً کد جدید دریافت کنید."));
        if (otp.getAttempts() >= settings.maxAttempts()) {
            throw ApiException.badRequest("OTP_TOO_MANY_ATTEMPTS",
                    "تعداد تلاش‌های ناموفق زیاد است. لطفاً کد جدید دریافت کنید.");
        }
        boolean matches = MessageDigest.isEqual(
                hash(code).getBytes(StandardCharsets.US_ASCII),
                otp.getCodeHash().getBytes(StandardCharsets.US_ASCII));
        if (!matches) {
            otp.recordFailedAttempt();
            throw ApiException.badRequest("OTP_INVALID", "کد تأیید اشتباه است.");
        }
        otp.consume(now);
    }

    @Transactional
    public int deleteOlderThan(Instant before) {
        return otpCodes.deleteCreatedBefore(before);
    }

    private String randomCode() {
        int bound = (int) Math.pow(10, settings.length());
        int lowest = bound / 10;   // no leading zero, so the code is always full length
        return String.valueOf(lowest + RANDOM.nextInt(bound - lowest));
    }

    /** HMAC-SHA256 keyed with a server secret: a six-digit code is trivial to brute-force from a plain hash. */
    private String hash(String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(settings.hashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(code.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
