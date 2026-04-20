package com.ecommerce.demo.controller;

import com.ecommerce.demo.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<String>> allAccess() {
        return ResponseEntity.ok(new ApiResponse<>(200, "Public Content", "This is public content."));
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> userAccess() {
        return ResponseEntity.ok(new ApiResponse<>(200, "User Content", "This is content for users and admins."));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> adminAccess() {
        return ResponseEntity.ok(new ApiResponse<>(200, "Admin Content", "This is content for admins only."));
    }
}
