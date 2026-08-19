package com.mka.service;

import com.mka.dto.request.UpdatePasswordRequest;
import com.mka.entity.User;
import com.mka.enums.Role;
import com.mka.exception.ValidationException;
import com.mka.mapper.UserMapper;
import com.mka.repository.UserRepository;
import com.mka.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("encoded_old_pass")
                .fullName("Test User")
                .role(Role.USER)
                .active(true)
                .deleted(false)
                .build();
    }

    @Test
    void testUpdatePassword_Success() {
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("old_pass123");
        request.setNewPassword("new_pass123");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("old_pass123", "encoded_old_pass")).thenReturn(true);
        when(passwordEncoder.encode("new_pass123")).thenReturn("encoded_new_pass");

        assertDoesNotThrow(() -> userService.updatePassword("user@example.com", request));

        verify(passwordEncoder).matches("old_pass123", "encoded_old_pass");
        verify(passwordEncoder).encode("new_pass123");
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdatePassword_WrongOldPassword_ThrowsValidationException() {
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("wrong_old_pass");
        request.setNewPassword("new_pass123");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong_old_pass", "encoded_old_pass")).thenReturn(false);

        assertThrows(ValidationException.class, () -> userService.updatePassword("user@example.com", request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testDeactivateUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        userService.deactivateUser(1L);

        assertFalse(testUser.getActive());
        assertTrue(testUser.getDeleted());
        verify(userRepository).save(testUser);
    }
}
