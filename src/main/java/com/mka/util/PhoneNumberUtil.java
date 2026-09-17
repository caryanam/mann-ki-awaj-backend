package com.mka.util;

import java.util.regex.Pattern;

public final class PhoneNumberUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern INDIAN_10_DIGIT_PATTERN = Pattern.compile("^[6-9]\\d{9}$");

    private PhoneNumberUtil() {
        // Private constructor for utility class
    }

    /**
     * Determines whether the given string represents an email address.
     */
    public static boolean isEmail(String input) {
        if (input == null) {
            return false;
        }
        String trimmed = input.trim();
        return trimmed.contains("@") && EMAIL_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Normalizes an Indian mobile number into canonical 10-digit format (e.g. 9876543210).
     * Strips whitespace, hyphens, parentheses, dots, leading '+91', '0091', '91' (if 12 digits), or leading '0'.
     * Returns the canonical 10-digit number if valid, or null if input cannot be normalized to a valid Indian mobile number.
     */
    public static String normalizeMobile(String input) {
        if (input == null) {
            return null;
        }
        String cleaned = input.replaceAll("[\\s\\-\\(\\)\\.]", "").trim();

        if (cleaned.startsWith("+91")) {
            cleaned = cleaned.substring(3);
        } else if (cleaned.startsWith("0091")) {
            cleaned = cleaned.substring(4);
        } else if (cleaned.startsWith("91") && cleaned.length() == 12 && isIndianStartingDigit(cleaned.charAt(2))) {
            cleaned = cleaned.substring(2);
        } else if (cleaned.startsWith("0") && cleaned.length() == 11 && isIndianStartingDigit(cleaned.charAt(1))) {
            cleaned = cleaned.substring(1);
        }

        if (INDIAN_10_DIGIT_PATTERN.matcher(cleaned).matches()) {
            return cleaned;
        }

        return null;
    }

    /**
     * Checks if the given input is a valid mobile number that can be normalized.
     */
    public static boolean isValidMobile(String input) {
        return normalizeMobile(input) != null;
    }

    private static boolean isIndianStartingDigit(char c) {
        return c == '6' || c == '7' || c == '8' || c == '9';
    }
}
