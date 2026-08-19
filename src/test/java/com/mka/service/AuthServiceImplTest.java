package com.mka.service;

import com.mka.config.JwtService;
import com.mka.dto.request.*;
import com.mka.dto.response.AuthResponse;
import com.mka.entity.EmailVerification;
import com.mka.entity.MobileVerification;
import com.mka.entity.User;
import com.mka.enums.Role;
import com.mka.exception.ResourceAlreadyExistsException;
import com.mka.exception.UnauthorizedException;
import com.mka.exception.ValidationException;
import com.mka.mapper.ProfileMapper;
import com.mka.repository.AdminRepository;
import com.mka.repository.EmailVerificationRepository;
import com.mka.repository.MobileVerificationRepository;
import com.mka.repository.ProfileRepository;
import com.mka.repository.UserRepository;
import com.mka.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private MobileVerificationRepository mobileVerificationRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private MobileVerificationService mobileVerificationService;

    @Mock
    private ProfileMapper profileMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .mobileNumber("9876543210")
                .password("encoded_pass")
                .fullName("Test User")
                .role(Role.USER)
                .active(true)
                .deleted(false)
                .emailVerified(true)
                .mobileVerified(true)
                .build();
    }

    @Test
    void testRegister_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setMobileNumber("9998887776");
        request.setPassword("password123");
        request.setFullName("New User");

        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByMobileNumber("9998887776")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        verify(userRepository).save(any(User.class));
        verify(emailVerificationService).sendVerificationOtp(any());
    }

    @Test
    void testRegister_DuplicateEmail_Verified_ThrowsResourceAlreadyExistsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setMobileNumber("9876543210");
        request.setPassword("password123");
        request.setFullName("Test User");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(ResourceAlreadyExistsException.class, () -> authService.register(request));
    }

    @Test
    void testLogin_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password123");

        when(adminRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken("user@example.com", "USER")).thenReturn("mock.jwt.token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("user@example.com", response.getEmail());
    }

    @Test
    void testLogin_WrongPassword_ThrowsBadCredentialsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("wrongpassword");

        when(adminRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        doThrow(new BadCredentialsException("Invalid credentials"))
                .when(authenticationManager).authenticate(any());

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void testLogin_UnverifiedAccount_ThrowsUnauthorizedException() {
        testUser.setEmailVerified(false);
        testUser.setMobileVerified(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password123");

        when(adminRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void testLogin_InactiveAccount_ThrowsUnauthorizedException() {
        testUser.setActive(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password123");

        when(adminRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void testForgotPassword_Success() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setIdentifier("user@example.com");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));

        authService.forgotPassword(request);

        verify(emailVerificationService).sendVerificationOtp(testUser);
    }

    @Test
    void testVerifyForgotPasswordOtp_ValidOtp_Success() {
        VerifyForgotPasswordOtpRequest request = new VerifyForgotPasswordOtpRequest();
        request.setIdentifier("user@example.com");
        request.setOtp("123456");

        EmailVerification verification = EmailVerification.builder()
                .user(testUser)
                .otp("123456")
                .expiryTime(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(emailVerificationRepository.findByUserAndOtpAndUsedFalse(testUser, "123456"))
                .thenReturn(Optional.of(verification));

        assertDoesNotThrow(() -> authService.verifyForgotPasswordOtp(request));
    }

    @Test
    void testResetPassword_ValidOtp_UpdatesPassword() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setIdentifier("user@example.com");
        request.setOtp("123456");
        request.setNewPassword("newpassword123");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newpassword123")).thenReturn("new_encoded_pass");

        authService.resetPassword(request);

        verify(emailVerificationService).verifyEmail(any());
        verify(passwordEncoder).encode("newpassword123");
        verify(userRepository).save(testUser);
    }

    @Test
    void testResetPassword_InvalidOtp_ThrowsValidationException() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setIdentifier("user@example.com");
        request.setOtp("000000");
        request.setNewPassword("newpassword123");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        doThrow(new ValidationException("Invalid or expired OTP"))
                .when(emailVerificationService).verifyEmail(any());

        assertThrows(ValidationException.class, () -> authService.resetPassword(request));
        verify(userRepository, never()).save(any());
    }
}
