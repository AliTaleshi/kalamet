package com.kalamet.common.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MobileNumberTest {

    @ParameterizedTest
    @ValueSource(strings = {"09121234567", "9121234567", "989121234567", "+989121234567", "00989121234567",
            "۰۹۱۲۱۲۳۴۵۶۷", "0912 123 4567", "0912-123-4567", "٠٩١٢١٢٣٤٥٦٧"})
    void normalizesCommonFormats(String input) {
        assertThat(MobileNumber.normalize(input)).isEqualTo("09121234567");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0912123456", "091212345678", "02112345678", "abc", "08121234567"})
    void rejectsNonMobileNumbers(String input) {
        assertThat(MobileNumber.normalize(input)).isNull();
    }

    @org.junit.jupiter.api.Test
    void masksMiddleDigits() {
        assertThat(MobileNumber.mask("09121234567")).isEqualTo("0912***4567");
    }
}
