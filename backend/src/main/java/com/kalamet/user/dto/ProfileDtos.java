package com.kalamet.user.dto;

import com.kalamet.user.domain.Role;
import com.kalamet.user.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Profile bodies. */
public final class ProfileDtos {

    private ProfileDtos() {
    }

    public record ProfileResponse(Long id, String mobile, String email, String firstName, String lastName,
                                  Role role, boolean profileComplete, Instant createdAt) {

        public static ProfileResponse of(User user) {
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
}
