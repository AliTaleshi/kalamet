package com.kalamet.user;

import com.kalamet.common.MobileNumber;
import com.kalamet.common.PersianText;
import com.kalamet.config.KalametProperties;
import com.kalamet.user.UserDtos.AuthResponse;
import com.kalamet.user.UserDtos.OtpRequest;
import com.kalamet.user.UserDtos.OtpResponse;
import com.kalamet.user.UserDtos.ProfileResponse;
import com.kalamet.user.UserDtos.RefreshRequest;
import com.kalamet.user.UserDtos.VerifyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final OtpService otpService;
    private final AuthService authService;
    private final TokenService tokenService;
    private final OtpIpLimiter ipLimiter;
    private final boolean demoMode;

    AuthController(OtpService otpService, AuthService authService, TokenService tokenService,
                   OtpIpLimiter ipLimiter, KalametProperties properties) {
        this.otpService = otpService;
        this.ipLimiter = ipLimiter;
        this.authService = authService;
        this.tokenService = tokenService;
        this.demoMode = properties.otp().demoMode();
    }

    @Operation(summary = "Send a login code by SMS")
    @PostMapping("/otp")
    OtpResponse requestCode(@Valid @RequestBody OtpRequest request, HttpServletRequest http) {
        String mobile = MobileNumber.require(request.mobile());
        ipLimiter.check(http.getRemoteAddr());
        OtpService.IssuedCode issued = otpService.issue(mobile);
        return new OtpResponse(mobile, issued.expiresInSeconds(), issued.resendInSeconds(),
                demoMode ? issued.code() : null);
    }

    @Operation(summary = "Check the code and sign in (creates the account on first sign-in)")
    @PostMapping("/verify")
    AuthResponse verify(@Valid @RequestBody VerifyRequest request) {
        String mobile = MobileNumber.require(request.mobile());
        AuthService.SignIn signIn = authService.verify(mobile, PersianText.asciiDigits(request.code()));
        return AuthResponse.of(signIn.tokens(), signIn.newUser(), ProfileResponse.of(signIn.user()));
    }

    @Operation(summary = "Exchange a refresh token for a new token pair")
    @PostMapping("/refresh")
    AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        TokenService.Tokens tokens = tokenService.refresh(request.refreshToken());
        return AuthResponse.of(tokens, false, null);
    }

    @Operation(summary = "Sign out: revoke the refresh token")
    @PostMapping("/logout")
    ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        tokenService.revoke(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
