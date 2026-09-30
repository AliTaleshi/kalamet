package com.kalamet.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Request and response bodies of the user feature. */
public final class UserDtos {

    private UserDtos() {
    }

    // --- Sign-in ---

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

        static AuthResponse of(TokenService.Tokens tokens, boolean newUser, ProfileResponse user) {
            return new AuthResponse(tokens.accessToken(), "Bearer", tokens.expiresInSeconds(),
                    tokens.refreshToken(), newUser, user);
        }
    }

    // --- Profile ---

    public record ProfileResponse(Long id, String mobile, String email, String firstName, String lastName,
                                  Role role, boolean profileComplete, Instant createdAt) {

        static ProfileResponse of(User user) {
            return new ProfileResponse(user.getId(), user.getMobile(), user.getEmail(), user.getFirstName(),
                    user.getLastName(), user.getRole(), user.profileComplete(), user.getCreatedAt());
        }
    }

    /** Fields left null are cleared, so send the whole profile. */
    public record ProfileUpdateRequest(
            @Size(max = 100, message = "نام حداکثر ۱۰۰ حرف است.") String firstName,
            @Size(max = 100, message = "نام خانوادگی حداکثر ۱۰۰ حرف است.") String lastName,
            @Email(message = "ایمیل معتبر نیست.") @Size(max = 255) String email) {
    }

    // --- Addresses ---

    public record ProvinceResponse(Short id, String name) {

        static ProvinceResponse of(Province province) {
            return new ProvinceResponse(province.getId(), province.getName());
        }
    }

    public record AddressRequest(
            @NotBlank(message = "نام گیرنده را وارد کنید.") @Size(max = 200) String recipientName,
            @NotBlank(message = "شماره موبایل گیرنده را وارد کنید.") String recipientMobile,
            @NotNull(message = "استان را انتخاب کنید.") Short provinceId,
            @NotBlank(message = "شهر را وارد کنید.") @Size(max = 100) String city,
            @NotBlank(message = "نشانی پستی را وارد کنید.") @Size(max = 1000) String addressLine,
            @NotBlank(message = "پلاک را وارد کنید.") @Size(max = 20) String plaque,
            @Size(max = 20) String unit,
            @NotBlank(message = "کد پستی را وارد کنید.") String postalCode,
            Boolean makeDefault) {

        boolean wantsDefault() {
            return Boolean.TRUE.equals(makeDefault);
        }
    }

    public record AddressResponse(Long id, String recipientName, String recipientMobile, ProvinceResponse province,
                                  String city, String addressLine, String plaque, String unit, String postalCode,
                                  boolean isDefault) {

        static AddressResponse of(Address address) {
            return new AddressResponse(address.getId(), address.getRecipientName(), address.getRecipientMobile(),
                    ProvinceResponse.of(address.getProvince()), address.getCity(), address.getAddressLine(),
                    address.getPlaque(), address.getUnit(), address.getPostalCode(), address.isDefaultAddress());
        }
    }

    // --- Admin ---

    public record AdminUserResponse(Long id, String mobile, String email, String firstName, String lastName,
                                    Role role, boolean active, Instant createdAt) {

        static AdminUserResponse of(User user) {
            return new AdminUserResponse(user.getId(), user.getMobile(), user.getEmail(), user.getFirstName(),
                    user.getLastName(), user.getRole(), user.isActive(), user.getCreatedAt());
        }
    }

    /** Null fields are left unchanged. */
    public record AdminUserUpdateRequest(Role role, Boolean active) {
    }
}
