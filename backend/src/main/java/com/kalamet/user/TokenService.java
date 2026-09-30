package com.kalamet.user;

import com.kalamet.common.ApiException;
import com.kalamet.config.KalametProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Issues access JWTs and manages rotating refresh tokens. */
@Service
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokens;
    private final KalametProperties.Jwt settings;
    private final Clock clock;

    TokenService(JwtEncoder jwtEncoder, RefreshTokenRepository refreshTokens, KalametProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.refreshTokens = refreshTokens;
        this.settings = properties.jwt();
        this.clock = clock;
    }

    public record Tokens(String accessToken, long expiresInSeconds, String refreshToken) {
    }

    /** Must run inside a transaction: stores the new refresh token. */
    Tokens issue(User user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(settings.issuer())
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(settings.accessTokenTtl()))
                .claim("role", user.getRole().name())
                .claim("mobile", user.getMobile())
                .build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        refreshTokens.save(new RefreshToken(user, sha256(refreshToken), now, now.plus(settings.refreshTokenTtl())));

        return new Tokens(accessToken, settings.accessTokenTtl().toSeconds(), refreshToken);
    }

    /** Swaps a valid refresh token for a new pair. Reusing an old token revokes every session of that user. */
    @Transactional(noRollbackFor = ApiException.class)
    public Tokens refresh(String refreshToken) {
        Instant now = clock.instant();
        RefreshToken stored = refreshTokens.findByTokenHash(sha256(refreshToken))
                .orElseThrow(TokenService::invalidRefreshToken);
        if (stored.revoked()) {
            refreshTokens.revokeAllForUser(stored.getUser().getId(), now);
            throw invalidRefreshToken();
        }
        if (stored.expired(now) || !stored.getUser().isActive()) {
            throw invalidRefreshToken();
        }
        stored.revoke(now);
        return issue(stored.getUser());
    }

    @Transactional
    public void revoke(String refreshToken) {
        refreshTokens.findByTokenHash(sha256(refreshToken)).ifPresent(token -> token.revoke(clock.instant()));
    }

    @Transactional
    public int deleteExpiredBefore(Instant before) {
        return refreshTokens.deleteExpiredBefore(before);
    }

    private static ApiException invalidRefreshToken() {
        return ApiException.unauthorized("INVALID_REFRESH_TOKEN", "نشست شما منقضی شده است. لطفاً دوباره وارد شوید.");
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
