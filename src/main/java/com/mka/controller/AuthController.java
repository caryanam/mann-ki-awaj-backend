package com.mka.controller;



import com.mka.dto.request.LoginRequest;

import com.mka.dto.request.RegisterRequest;

import com.mka.dto.responce.ApiResponse;

import com.mka.dto.responce.AuthResponse;

import com.mka.dto.responce.LoginResponseDTO;

import com.mka.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;



@RestController

@RequestMapping("/api/auth")

@Tag(name = "Authentication", description = "Authentication APIs")

@RequiredArgsConstructor

public class AuthController {



    private final AuthService authService;



    @PostMapping("/register")

    @Operation(summary = "User Registration")

    public ResponseEntity<ApiResponse<LoginResponseDTO>> register(

            @Valid @RequestBody RegisterRequest request) {



        return ResponseEntity.ok(

                ApiResponse.<LoginResponseDTO>builder()

                        .success(true)

                        .message("User Registered Successfully")

                        .data(authService.register(request))

                        .build()

        );

    }



    @PostMapping("/login")

    @Operation(summary = "All Login")

    public ResponseEntity<ApiResponse<AuthResponse>> login(

            @Valid @RequestBody LoginRequest request) {



        return ResponseEntity.ok(

                ApiResponse.<AuthResponse>builder()

                        .success(true)

                        .message("Login Successfully")

                        .data(authService.login(request))

                        .build()

        );

    }

}


