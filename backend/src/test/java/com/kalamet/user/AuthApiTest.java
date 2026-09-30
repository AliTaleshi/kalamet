package com.kalamet.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.kalamet.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthApiTest extends IntegrationTest {

    @Test
    void firstSignInCreatesTheAccount() throws Exception {
        String mobile = newMobile();
        String code = requestCode(mobile);

        // Persian digits and a +98 prefix are accepted.
        String persianMobile = "+98" + toPersianDigits(mobile.substring(1));
        mvc.perform(post("/api/auth/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobile\":\"%s\",\"code\":\"%s\"}".formatted(persianMobile, toPersianDigits(code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newUser").value(true))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.mobile").value(mobile))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.profileComplete").value(false));
    }

    @Test
    void wrongCodesAreCountedAndLimited() throws Exception {
        String mobile = newMobile();
        String code = requestCode(mobile);
        String wrong = code.equals("111111") ? "222222" : "111111";

        for (int i = 0; i < 5; i++) {
            verify(mobile, wrong).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("OTP_INVALID"));
        }
        // Even the right code is refused once the attempts are used up.
        verify(mobile, code).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OTP_TOO_MANY_ATTEMPTS"));
    }

    @Test
    void codeWorksOnlyOnce() throws Exception {
        String mobile = newMobile();
        String code = requestCode(mobile);
        verify(mobile, code).andExpect(status().isOk());
        verify(mobile, code).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("OTP_EXPIRED"));
    }

    @Test
    void resendingTooSoonIsRejected() throws Exception {
        String mobile = newMobile();
        requestCode(mobile);
        mvc.perform(post("/api/auth/otp").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobile\":\"%s\"}".formatted(mobile)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("OTP_RESEND_TOO_SOON"));
    }

    @Test
    void invalidMobileIsRejected() throws Exception {
        mvc.perform(post("/api/auth/otp").contentType(MediaType.APPLICATION_JSON).content("{\"mobile\":\"02112345678\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MOBILE"));
    }

    @Test
    void refreshTokensRotateAndReuseRevokesEverything() throws Exception {
        Session session = signInCustomer();

        String refreshed = body(refresh(session.refreshToken()).andExpect(status().isOk()));
        String newRefreshToken = JsonPath.read(refreshed, "$.refreshToken");
        assertThat(newRefreshToken).isNotEqualTo(session.refreshToken());

        // The old token was rotated away; presenting it again looks like theft.
        refresh(session.refreshToken()).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        // ...so the token issued in the meantime is revoked too.
        refresh(newRefreshToken).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheRefreshToken() throws Exception {
        Session session = signInCustomer();
        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(session.refreshToken())))
                .andExpect(status().isNoContent());
        refresh(session.refreshToken()).andExpect(status().isUnauthorized());
    }

    @Test
    void configuredMobileBecomesAdmin() throws Exception {
        Session admin = signInAdmin();
        getAs("/api/me", admin).andExpect(jsonPath("$.role").value("ADMIN"));
        getAs("/api/admin/users", admin).andExpect(status().isOk());
    }

    @Test
    void protectedEndpointsNeedATokenAndTheRightRole() throws Exception {
        getAs("/api/me", null).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        getAs("/api/cart", null).andExpect(status().isUnauthorized());
        getAs("/api/admin/orders", signInCustomer()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void disabledUsersCannotSignIn() throws Exception {
        String mobile = newMobile();
        Session customer = signIn(mobile);
        long userId = ((Number) JsonPath.read(body(getAs("/api/me", customer)), "$.id")).longValue();

        patchAs("/api/admin/users/" + userId, signInAdmin(), "{\"active\": false}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));

        refresh(customer.refreshToken()).andExpect(status().isUnauthorized());
        jdbc.sql("DELETE FROM otp_codes WHERE mobile = :mobile").param("mobile", mobile).update();
        verify(mobile, requestCode(mobile)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    private String requestCode(String mobile) throws Exception {
        String response = body(mvc.perform(post("/api/auth/otp").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobile\":\"%s\"}".formatted(mobile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresInSeconds").value(120)));
        return JsonPath.read(response, "$.demoCode");
    }

    private org.springframework.test.web.servlet.ResultActions verify(String mobile, String code) throws Exception {
        return mvc.perform(post("/api/auth/verify").contentType(MediaType.APPLICATION_JSON)
                .content("{\"mobile\":\"%s\",\"code\":\"%s\"}".formatted(mobile, code)));
    }

    private org.springframework.test.web.servlet.ResultActions refresh(String token) throws Exception {
        return mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(token)));
    }

    private static String toPersianDigits(String digits) {
        StringBuilder out = new StringBuilder();
        digits.chars().forEach(c -> out.append((char) ('\u06F0' + (c - '0'))));
        return out.toString();
    }
}
