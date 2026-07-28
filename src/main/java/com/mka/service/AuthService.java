package com.mka.service;







import com.mka.dto.request.LoginRequest;

import com.mka.dto.request.RegisterRequest;

import com.mka.dto.responce.AuthResponse;

import com.mka.dto.responce.LoginResponseDTO;





public interface AuthService {



    LoginResponseDTO register(RegisterRequest request);



    AuthResponse login(LoginRequest request);



}


