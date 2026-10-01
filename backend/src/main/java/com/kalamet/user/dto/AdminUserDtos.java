package com.kalamet.user.dto;

import com.kalamet.user.domain.Role;
import com.kalamet.user.domain.User;
import java.time.Instant;

/** Admin user-management bodies. */
public final class AdminUserDtos {

    private AdminUserDtos() {
    }

    public record AdminUserResponse(Long id, String mobile, String email, String firstName, String lastName,
                                    Role role, boolean active, Instant createdAt) {

        public static AdminUserResponse of(User user) {
            return new AdminUserResponse(user.getId(), user.getMobile(), user.getEmail(), user.getFirstName(),
                    user.getLastName(), user.getRole(), user.isActive(), user.getCreatedAt());
        }
    }

    /** Null fields are left unchanged. */
    public record AdminUserUpdateRequest(Role role, Boolean active) {
    }
}
