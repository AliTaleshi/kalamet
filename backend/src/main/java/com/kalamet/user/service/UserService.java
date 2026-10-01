package com.kalamet.user.service;

import com.kalamet.common.error.ApiException;
import com.kalamet.common.text.PersianText;
import com.kalamet.common.web.PageResponse;
import com.kalamet.user.domain.Role;
import com.kalamet.user.domain.User;
import com.kalamet.user.dto.AdminUserDtos.AdminUserResponse;
import com.kalamet.user.dto.AdminUserDtos.AdminUserUpdateRequest;
import com.kalamet.user.dto.ProfileDtos.ProfileResponse;
import com.kalamet.user.dto.ProfileDtos.ProfileUpdateRequest;
import com.kalamet.user.repository.RefreshTokenRepository;
import com.kalamet.user.repository.UserRepository;
import java.time.Clock;
import java.util.Locale;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;

    UserService(UserRepository users, RefreshTokenRepository refreshTokens, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

    /** For other features that need to attach a user reference. */
    @Transactional(readOnly = true)
    public User require(Long userId) {
        return users.findById(userId).orElseThrow(UserService::notFound);
    }

    @Transactional(readOnly = true)
    public ProfileResponse profile(Long userId) {
        return ProfileResponse.of(require(userId));
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = require(userId);
        String email = request.email() == null || request.email().isBlank()
                ? null : request.email().strip().toLowerCase(Locale.ROOT);
        if (email != null && users.existsByEmailAndIdNot(email, userId)) {
            throw ApiException.conflict("EMAIL_TAKEN", "این ایمیل قبلاً ثبت شده است.");
        }
        user.setFirstName(PersianText.normalize(request.firstName()));
        user.setLastName(PersianText.normalize(request.lastName()));
        user.setEmail(email);
        return ProfileResponse.of(users.saveAndFlush(user));
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> search(String query, Role role, Pageable pageable) {
        String normalized = query == null || query.isBlank() ? null
                : PersianText.digitsToAscii(PersianText.normalize(query));
        return PageResponse.of(users.search(normalized, role, pageable), AdminUserResponse::of);
    }

    @Transactional
    public AdminUserResponse adminUpdate(Long adminId, Long userId, AdminUserUpdateRequest request) {
        User user = require(userId);
        if (adminId.equals(userId)) {
            throw ApiException.badRequest("CANNOT_CHANGE_SELF", "نمی‌توانید نقش یا وضعیت حساب خودتان را تغییر دهید.");
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.active() != null) {
            user.setActive(request.active());
            if (!request.active()) {
                // Existing access tokens expire within minutes; refresh tokens stop working now.
                refreshTokens.revokeAllForUser(userId, clock.instant());
            }
        }
        return AdminUserResponse.of(users.saveAndFlush(user));
    }

    private static ApiException notFound() {
        return ApiException.notFound("USER_NOT_FOUND", "کاربر پیدا نشد.");
    }
}
