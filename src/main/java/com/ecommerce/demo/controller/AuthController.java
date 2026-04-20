package com.ecommerce.demo.controller;

import com.ecommerce.demo.dto.request.LoginRequest;
import com.ecommerce.demo.dto.request.RegisterRequest;
import com.ecommerce.demo.dto.request.TokenRefreshRequest;
import com.ecommerce.demo.dto.response.ApiResponse;
import com.ecommerce.demo.dto.response.AuthResponse;
import com.ecommerce.demo.service.AuthService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(201, "User registered successfully", authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(200, "Login successful", authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(200, "Token refreshed successfully", authService.refreshToken(request)));
    }
}
