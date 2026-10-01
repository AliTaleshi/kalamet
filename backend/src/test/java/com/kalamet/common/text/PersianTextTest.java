package com.kalamet.common.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PersianTextTest {

    @Test
    void replacesArabicYehAndKaf() {
        assertThat(PersianText.normalize(" \u0643تاب عل\u064A ")).isEqualTo("کتاب علی");
    }

    @Test
    void blankBecomesNull() {
        assertThat(PersianText.normalize("   ")).isNull();
        assertThat(PersianText.normalize(null)).isNull();
    }

    @Test
    void convertsPersianAndArabicDigits() {
        assertThat(PersianText.asciiDigits("۱۲۳-٤٥٦ ۷")).isEqualTo("1234567");
    }
}
