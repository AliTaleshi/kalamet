package com.kalamet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Base for API tests against a real PostgreSQL with the migrations and demo catalog.
 * OTP demo mode is on, so sign-in reads the code from the response instead of an SMS.
 */
@SpringBootTest(properties = {
        "kalamet.jobs.enabled=false",
        "kalamet.otp.demo-mode=true",
        // Every MockMvc request comes from 127.0.0.1; OtpIpLimiterTest covers the per-IP limit.
        "kalamet.otp.max-per-ip=1000000",
        "kalamet.admin-mobiles=" + IntegrationTest.ADMIN_MOBILE})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    public static final String ADMIN_MOBILE = "09120000000";
    private static final AtomicInteger MOBILE_SEQUENCE = new AtomicInteger((int) (System.nanoTime() % 1_000_000));
    private static Session adminSession;

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcClient jdbc;

    public record Session(String accessToken, String refreshToken) {
    }

    /** A mobile number no other test uses, so OTP rate limits never collide. */
    protected static String newMobile() {
        return "0935" + String.format("%07d", MOBILE_SEQUENCE.incrementAndGet() % 10_000_000);
    }

    protected Session signIn(String mobile) throws Exception {
        String otp = body(mvc.perform(json(post("/api/auth/otp"), "{\"mobile\":\"%s\"}".formatted(mobile)))
                .andExpect(status().isOk()));
        String code = JsonPath.read(otp, "$.demoCode");
        String auth = body(mvc.perform(json(post("/api/auth/verify"),
                        "{\"mobile\":\"%s\",\"code\":\"%s\"}".formatted(mobile, code)))
                .andExpect(status().isOk()));
        return new Session(JsonPath.read(auth, "$.accessToken"), JsonPath.read(auth, "$.refreshToken"));
    }

    protected Session signInCustomer() throws Exception {
        return signIn(newMobile());
    }

    /** Signed in once per test run: the OTP resend limit allows one code per two minutes. */
    protected Session signInAdmin() throws Exception {
        synchronized (IntegrationTest.class) {
            if (adminSession == null) {
                adminSession = signIn(ADMIN_MOBILE);
            }
            return adminSession;
        }
    }

    /** Creates a default address and returns its id. */
    protected long createAddress(Session session) throws Exception {
        String response = body(call(post("/api/me/addresses"), session, """
                {"recipientName": "علی رضایی", "recipientMobile": "09121234567", "provinceId": 8,
                 "city": "تهران", "addressLine": "خیابان ولیعصر، کوچه نسترن", "plaque": "۱۲",
                 "postalCode": "۱۲۳۴۵۶۷۸۹۰", "makeDefault": true}
                """).andExpect(status().isCreated()));
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    protected long variantId(String sku) {
        return jdbc.sql("SELECT id FROM product_variants WHERE sku = :sku").param("sku", sku)
                .query(Long.class).single();
    }

    protected int stock(String sku) {
        return jdbc.sql("SELECT stock_quantity FROM product_variants WHERE sku = :sku").param("sku", sku)
                .query(Integer.class).single();
    }

    protected ResultActions call(MockHttpServletRequestBuilder request, Session session) throws Exception {
        return mvc.perform(authorized(request, session));
    }

    protected ResultActions call(MockHttpServletRequestBuilder request, Session session, String jsonBody)
            throws Exception {
        return mvc.perform(json(authorized(request, session), jsonBody));
    }

    protected ResultActions getAs(String path, Session session) throws Exception {
        return call(get(path), session);
    }

    protected ResultActions postAs(String path, Session session, String jsonBody) throws Exception {
        return call(post(path), session, jsonBody);
    }

    protected ResultActions putAs(String path, Session session, String jsonBody) throws Exception {
        return call(put(path), session, jsonBody);
    }

    protected ResultActions patchAs(String path, Session session, String jsonBody) throws Exception {
        return call(patch(path), session, jsonBody);
    }

    protected ResultActions deleteAs(String path, Session session) throws Exception {
        return call(delete(path), session);
    }

    protected static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request, Session session) {
        return session == null ? request : request.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken());
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
