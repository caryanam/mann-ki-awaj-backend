package com.mka.service;

import com.mka.dto.request.UpdatePasswordRequest;
import com.mka.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse getUserById(Long id);

    UserResponse getUserByEmail(String email);

    UserResponse getCurrentUser(String email);

    Page<UserResponse> getAllUsers(Pageable pageable);

    void updatePassword(String email, UpdatePasswordRequest request);

    void deactivateUser(Long id);

    void activateUser(Long id);
}
