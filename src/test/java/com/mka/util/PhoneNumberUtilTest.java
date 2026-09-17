package com.mka.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhoneNumberUtilTest {

    @Test
    void testNormalizeIndianMobileNumber_VariousValidFormats() {
        assertEquals("9876543210", PhoneNumberUtil.normalizeMobile("9876543210"));
        assertEquals("9876543210", PhoneNumberUtil.normalizeMobile("+919876543210"));
        assertEquals("9876543210", PhoneNumberUtil.normalizeMobile("+91 9876543210"));
        assertEquals("9876543210", PhoneNumberUtil.normalizeMobile("+91-98765-43210"));
        assertEquals("9876543210", PhoneNumberUtil.normalizeMobile("09876543210"));
        assertEquals("9876543210", PhoneNumberUtil.normalizeMobile("919876543210"));
        assertEquals("9876543210", PhoneNumberUtil.normalizeMobile("00919876543210"));
        assertEquals("6123456789", PhoneNumberUtil.normalizeMobile("+91 61234 56789"));
        assertEquals("7987654321", PhoneNumberUtil.normalizeMobile("7987654321"));
        assertEquals("8888888888", PhoneNumberUtil.normalizeMobile("+918888888888"));
    }

    @Test
    void testNormalizeIndianMobileNumber_InvalidInputs() {
        assertNull(PhoneNumberUtil.normalizeMobile("123"));
        assertNull(PhoneNumberUtil.normalizeMobile("abcdefghij"));
        assertNull(PhoneNumberUtil.normalizeMobile("0000000000"));
        assertNull(PhoneNumberUtil.normalizeMobile("random text"));
        assertNull(PhoneNumberUtil.normalizeMobile("5555555555")); // Indian mobile numbers start with 6-9
        assertNull(PhoneNumberUtil.normalizeMobile(""));
        assertNull(PhoneNumberUtil.normalizeMobile(null));
        assertNull(PhoneNumberUtil.normalizeMobile("98765432100")); // 11 digits without 0
    }

    @Test
    void testIsEmail() {
        assertTrue(PhoneNumberUtil.isEmail("user@gmail.com"));
        assertTrue(PhoneNumberUtil.isEmail("test.user+tag@domain.co.in"));
        assertFalse(PhoneNumberUtil.isEmail("9876543210"));
        assertFalse(PhoneNumberUtil.isEmail("+919876543210"));
        assertFalse(PhoneNumberUtil.isEmail("invalid-email"));
        assertFalse(PhoneNumberUtil.isEmail(""));
        assertFalse(PhoneNumberUtil.isEmail(null));
    }

    @Test
    void testIsValidMobile() {
        assertTrue(PhoneNumberUtil.isValidMobile("9876543210"));
        assertTrue(PhoneNumberUtil.isValidMobile("+919876543210"));
        assertFalse(PhoneNumberUtil.isValidMobile("user@example.com"));
        assertFalse(PhoneNumberUtil.isValidMobile("12345"));
    }
}
