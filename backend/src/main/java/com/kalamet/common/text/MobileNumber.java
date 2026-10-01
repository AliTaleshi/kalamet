package com.kalamet.common.text;

import com.kalamet.common.error.ApiException;
import java.util.regex.Pattern;

/** Iranian mobile numbers in the canonical {@code 09xxxxxxxxx} form used by the database. */
public final class MobileNumber {

    private static final Pattern CANONICAL = Pattern.compile("^09[0-9]{9}$");

    private MobileNumber() {
    }

    /**
     * Accepts 09121234567, 9121234567, 989121234567, +989121234567, 00989121234567, with
     * Persian digits, spaces or dashes. Returns null when the input is not a mobile number.
     */
    public static String normalize(String input) {
        String digits = PersianText.asciiDigits(input);
        if (digits == null) {
            return null;
        }
        if (digits.startsWith("+98")) {
            digits = "0" + digits.substring(3);
        } else if (digits.startsWith("0098")) {
            digits = "0" + digits.substring(4);
        } else if (digits.startsWith("98") && digits.length() == 12) {
            digits = "0" + digits.substring(2);
        } else if (digits.startsWith("9") && digits.length() == 10) {
            digits = "0" + digits;
        }
        return CANONICAL.matcher(digits).matches() ? digits : null;
    }

    public static String require(String input) {
        String mobile = normalize(input);
        if (mobile == null) {
            throw ApiException.badRequest("INVALID_MOBILE", "شماره موبایل معتبر نیست.");
        }
        return mobile;
    }

    /** 0912***4567, for logs and admin lists. */
    public static String mask(String mobile) {
        return mobile == null || mobile.length() != 11 ? mobile : mobile.substring(0, 4) + "***" + mobile.substring(7);
    }
}
