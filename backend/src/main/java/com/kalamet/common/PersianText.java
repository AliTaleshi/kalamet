package com.kalamet.common;

/**
 * Normalises user and admin input to standard Persian: Arabic Yeh/Kaf become Persian
 * ی/ک, and Persian or Arabic-Indic digits become ASCII digits where digits are expected.
 */
public final class PersianText {

    private static final char ZERO = '\u06F0';   // ۰

    private PersianText() {
    }

    /** Free text: fixes Arabic letters and trims. Returns null for null or blank input. */
    public static String normalize(String text) {
        if (text == null) {
            return null;
        }
        String result = text
                .replace('\u064A', '\u06CC')   // Arabic Yeh -> Persian Yeh
                .replace('\u0649', '\u06CC')   // Alef Maksura -> Persian Yeh
                .replace('\u0643', '\u06A9')   // Arabic Kaf -> Persian Kaf
                .strip();
        return result.isEmpty() ? null : result;
    }

    /** Formats a number with Persian digits, for numbers inside Persian messages. */
    public static String digits(long number) {
        StringBuilder out = new StringBuilder();
        for (char c : Long.toString(number).toCharArray()) {
            out.append(c >= '0' && c <= '9' ? (char) (c - '0' + ZERO) : c);
        }
        return out.toString();
    }

    /** Converts ۰-۹ and ٠-٩ to 0-9 and strips spaces and dashes. */
    public static String asciiDigits(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder out = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (c >= '\u06F0' && c <= '\u06F9') {
                out.append((char) ('0' + (c - '\u06F0')));
            } else if (c >= '\u0660' && c <= '\u0669') {
                out.append((char) ('0' + (c - '\u0660')));
            } else if (c != ' ' && c != '-' && c != '\u200C') {
                out.append(c);
            }
        }
        return out.toString();
    }
}
