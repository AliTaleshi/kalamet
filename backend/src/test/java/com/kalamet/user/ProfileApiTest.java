package com.kalamet.user;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.kalamet.IntegrationTest;
import org.junit.jupiter.api.Test;

class ProfileApiTest extends IntegrationTest {

    @Test
    void profileIsNormalized() throws Exception {
        Session session = signInCustomer();
        // Arabic Yeh and Kaf in the input are stored as Persian Yeh and Kaf; the email is lower-cased.
        putAs("/api/me", session, """
                {"firstName": "عل\u064A", "lastName": "کریمی", "email": "Ali@Example.COM"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("علی"))
                .andExpect(jsonPath("$.email").value("ali@example.com"))
                .andExpect(jsonPath("$.profileComplete").value(true));
    }

    @Test
    void addressesKeepExactlyOneDefault() throws Exception {
        Session session = signInCustomer();
        long first = createAddress(session);
        long second = createAddress(session);

        getAs("/api/me/addresses", session)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(second))
                .andExpect(jsonPath("$[0].isDefault").value(true))
                .andExpect(jsonPath("$[1].isDefault").value(false))
                .andExpect(jsonPath("$[0].postalCode").value("1234567890"))
                .andExpect(jsonPath("$[0].province.name").value("تهران"));

        postAs("/api/me/addresses/" + first + "/default", session, "").andExpect(status().isOk());
        getAs("/api/me/addresses", session).andExpect(jsonPath("$[0].id").value(first));

        // Deleting the default promotes the remaining address.
        deleteAs("/api/me/addresses/" + first, session).andExpect(status().isNoContent());
        getAs("/api/me/addresses", session)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].isDefault").value(true));
    }

    @Test
    void addressValidationMessagesArePersian() throws Exception {
        Session session = signInCustomer();
        String response = body(postAs("/api/me/addresses", session, """
                {"recipientName": "", "recipientMobile": "0912", "provinceId": 8, "city": "تهران",
                 "addressLine": "x", "plaque": "1", "postalCode": "123"}
                """).andExpect(status().isBadRequest()));
        String message = JsonPath.read(response, "$.errors.recipientName");
        org.assertj.core.api.Assertions.assertThat(message).isEqualTo("نام گیرنده را وارد کنید.");
    }

    @Test
    void plaqueKeepsItsWordsAndGetsAsciiDigits() throws Exception {
        Session session = signInCustomer();
        postAs("/api/me/addresses", session, """
                {"recipientName": "علی", "recipientMobile": "09121234567", "provinceId": 8, "city": "تهران",
                 "addressLine": "خیابان آزادی", "plaque": "۱۲ الف", "unit": "طبقه ۳ - واحد ۷", "postalCode": "1234567890"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plaque").value("12 الف"))
                .andExpect(jsonPath("$.unit").value("طبقه 3 - واحد 7"));
    }

    @Test
    void usersCannotSeeOthersAddresses() throws Exception {
        long address = createAddress(signInCustomer());
        deleteAs("/api/me/addresses/" + address, signInCustomer()).andExpect(status().isNotFound());
    }
}
