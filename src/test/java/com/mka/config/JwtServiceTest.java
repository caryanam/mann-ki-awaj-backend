package com.mka.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secret", "mySuperSecretKeyForJwtSigningThatIsAtLeast256BitsLongForHMACSHA256Algorithm");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L); // 24 hours
    }

    @Test
    void testGenerateAndValidateToken_ActiveUser_Success() {
        String token = jwtService.generateToken("testuser@example.com", "USER");
        assertNotNull(token);

        UserDetails userDetails = new User(
                "testuser@example.com",
                "password",
                true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        assertTrue(jwtService.validateToken(token, userDetails));
        assertEquals("testuser@example.com", jwtService.extractUsername(token));
    }

    @Test
    void testValidateToken_InactiveUser_ReturnsFalse() {
        String token = jwtService.generateToken("testuser@example.com", "USER");

        UserDetails inactiveUser = new User(
                "testuser@example.com",
                "password",
                false, // disabled / inactive
                true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        assertFalse(jwtService.validateToken(token, inactiveUser));
    }
}
