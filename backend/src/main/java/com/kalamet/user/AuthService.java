package com.kalamet.user;

import com.kalamet.common.ApiException;
import com.kalamet.common.MobileNumber;
import com.kalamet.config.KalametProperties;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sign-in with mobile number and SMS code. The account is created on the first successful sign-in. */
@Slf4j
@Service
public class AuthService {

    private final OtpService otpService;
    private final TokenService tokenService;
    private final UserRepository users;
    private final Set<String> adminMobiles;

    AuthService(OtpService otpService, TokenService tokenService, UserRepository users, KalametProperties properties) {
        this.otpService = otpService;
        this.tokenService = tokenService;
        this.users = users;
        this.adminMobiles = properties.adminMobiles().stream()
                .map(MobileNumber::normalize)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    public record SignIn(TokenService.Tokens tokens, boolean newUser, User user) {
    }

    @Transactional(noRollbackFor = ApiException.class)
    public SignIn verify(String mobile, String code) {
        otpService.verify(mobile, code);

        User user = users.findByMobile(mobile).orElse(null);
        boolean newUser = user == null;
        if (newUser) {
            user = users.save(new User(mobile));
        }
        if (!user.isActive()) {
            throw ApiException.forbidden("ACCOUNT_DISABLED", "حساب کاربری شما غیرفعال شده است.");
        }
        if (adminMobiles.contains(mobile) && user.getRole() != Role.ADMIN) {
            log.info("Granting ADMIN to configured mobile {}", user.getId());
            user.setRole(Role.ADMIN);
            users.flush();
        }
        return new SignIn(tokenService.issue(user), newUser, user);
    }
}
