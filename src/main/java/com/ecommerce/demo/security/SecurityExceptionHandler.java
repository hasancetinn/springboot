package com.ecommerce.demo.security;

import com.ecommerce.demo.dto.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException)
            throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        ApiResponse<Object> apiResponse = new ApiResponse<>(
                HttpStatus.UNAUTHORIZED.value(),
                "Kimlik doğrulama başarısız. Lütfen giriş yapın veya geçerli bir token kullanın.",
                authException.getMessage());

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    // 403 Forbidden - Yetki yetersiz (Rol uyuşmuyor)
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");

        ApiResponse<Object> apiResponse = new ApiResponse<>(
                HttpStatus.FORBIDDEN.value(),
                "Bu işlem için yetkiniz bulunmamaktadır.",
                accessDeniedException.getMessage());

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
