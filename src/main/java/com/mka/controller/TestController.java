package com.mka.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @GetMapping("/api/test")
    public String test() {
        return "JWT Authentication Successful";
    }

    @GetMapping("/api/user/test")
    @PreAuthorize("hasRole('USER')")
    public String userApi() {
        return "USER API";
    }

    @GetMapping("/api/admin/test")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminApi() {
        return "ADMIN API";
    }
}
