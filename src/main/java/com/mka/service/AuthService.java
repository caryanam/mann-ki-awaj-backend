package com.mka.service;

import com.mka.dto.request.LoginRequest;
import com.mka.dto.request.RegisterRequest;
import com.mka.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
