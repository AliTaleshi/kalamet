package com.kalamet.user.dto;

import com.kalamet.user.dto.ProfileDtos.ProfileResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Sign-in request and response bodies. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record OtpRequest(@NotBlank(message = "شماره موبایل را وارد کنید.") String mobile) {
    }

    /** {@code demoCode} is only filled in when kalamet.otp.demo-mode is on. */
    public record OtpResponse(String mobile, long expiresInSeconds, long resendInSeconds, String demoCode) {
    }

    public record VerifyRequest(
            @NotBlank(message = "شماره موبایل را وارد کنید.") String mobile,
            @NotBlank(message = "کد تأیید را وارد کنید.") @Size(max = 10) String code) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds, String refreshToken,
                               boolean newUser, ProfileResponse user) {

        public static AuthResponse bearer(String accessToken, long expiresInSeconds, String refreshToken,
                                          boolean newUser, ProfileResponse user) {
            return new AuthResponse(accessToken, "Bearer", expiresInSeconds, refreshToken, newUser, user);
        }
    }
}
