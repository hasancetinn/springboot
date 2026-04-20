package com.ecommerce.demo.controller;

import com.ecommerce.demo.dto.request.LoginRequest;
import com.ecommerce.demo.dto.request.RegisterRequest;
import com.ecommerce.demo.dto.request.TokenRefreshRequest;
import com.ecommerce.demo.dto.response.ApiResponse;
import com.ecommerce.demo.dto.response.AuthResponse;
import com.ecommerce.demo.exception.AuthException;
import com.ecommerce.demo.exception.TokenException;
import com.ecommerce.demo.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthController için saf Mockito unit testleri.
 *
 * Spring context yüklemez — controller metodları doğrudan çağrılır,
 * AuthService mock'lanır.
 *
 * NOT: @Valid (javax validation) Spring MVC olmadan çalışmaz.
 * Validation logic için ayrıca AuthService testlerine bakınız.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Unit Tests")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private AuthResponse mockAuthResponse;

    @BeforeEach
    void setUp() {
        mockAuthResponse = AuthResponse.builder()
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .build();
    }

    // ----------------------------------------------------------------
    // register() testleri
    // ----------------------------------------------------------------

    @Test
    @DisplayName("register() → 201 Created döndürmeli (başarılı kayıt)")
    void register_shouldReturn201_whenRequestIsValid() {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(mockAuthResponse);

        ResponseEntity<ApiResponse<AuthResponse>> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(201);
        assertThat(response.getBody().getMessage()).isEqualTo("User registered successfully");
        assertThat(response.getBody().getData().getAccessToken()).isEqualTo("mock-access-token");
        assertThat(response.getBody().getData().getRefreshToken()).isEqualTo("mock-refresh-token");

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("register() → AuthException fırlatmalı (kullanıcı adı zaten var)")
    void register_shouldThrowAuthException_whenUsernameAlreadyTaken() {
        RegisterRequest request = RegisterRequest.builder()
                .username("existinguser")
                .email("existing@example.com")
                .password("password123")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new AuthException("Username is already taken", HttpStatus.CONFLICT));

        assertThatThrownBy(() -> authController.register(request))
                .isInstanceOf(AuthException.class)
                .hasMessage("Username is already taken");

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("register() → AuthException fırlatmalı (email zaten kullanımda)")
    void register_shouldThrowAuthException_whenEmailAlreadyInUse() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("taken@example.com")
                .password("password123")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new AuthException("Email is already in use", HttpStatus.CONFLICT));

        assertThatThrownBy(() -> authController.register(request))
                .isInstanceOf(AuthException.class)
                .hasMessage("Email is already in use");
    }

    @Test
    @DisplayName("register() → response body'de accessToken ve refreshToken bulunmalı")
    void register_shouldReturnBothTokensInBody() {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();

        AuthResponse expectedResponse = AuthResponse.builder()
                .accessToken("eyJhbGciOiJIUzI1NiJ9.access")
                .refreshToken("eyJhbGciOiJIUzI1NiJ9.refresh")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(expectedResponse);

        ResponseEntity<ApiResponse<AuthResponse>> response = authController.register(request);

        assertThat(response.getBody().getData().getAccessToken()).isEqualTo("eyJhbGciOiJIUzI1NiJ9.access");
        assertThat(response.getBody().getData().getRefreshToken()).isEqualTo("eyJhbGciOiJIUzI1NiJ9.refresh");
    }

    // ----------------------------------------------------------------
    // login() testleri
    // ----------------------------------------------------------------

    @Test
    @DisplayName("login() → 200 OK döndürmeli (başarılı giriş)")
    void login_shouldReturn200_whenCredentialsAreValid() {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

        ResponseEntity<ApiResponse<AuthResponse>> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("Login successful");
        assertThat(response.getBody().getData().getAccessToken()).isEqualTo("mock-access-token");

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("login() → exception fırlatmalı (yanlış şifre)")
    void login_shouldThrowException_whenPasswordIsWrong() {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("wrongpassword")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authController.login(request))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("login() → AuthException fırlatmalı (kullanıcı bulunamadı)")
    void login_shouldThrowAuthException_whenUserNotFound() {
        LoginRequest request = LoginRequest.builder()
                .username("nonexistent")
                .password("password123")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AuthException("User not found", HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> authController.login(request))
                .isInstanceOf(AuthException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("login() → request nesnesi service'e iletilmeli")
    void login_shouldPassRequestToService() {
        LoginRequest request = LoginRequest.builder()
                .username("specificuser")
                .password("specificpass")
                .build();

        when(authService.login(request)).thenReturn(mockAuthResponse);

        authController.login(request);

        // Tam olarak aynı request nesnesi service'e gitmeli
        verify(authService).login(request);
    }

    // ----------------------------------------------------------------
    // refresh() testleri
    // ----------------------------------------------------------------

    @Test
    @DisplayName("refresh() → 200 OK döndürmeli (geçerli refresh token)")
    void refresh_shouldReturn200_whenTokenIsValid() {
        TokenRefreshRequest request = new TokenRefreshRequest("valid-refresh-token");

        AuthResponse newTokens = AuthResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .build();

        when(authService.refreshToken(any(TokenRefreshRequest.class))).thenReturn(newTokens);

        ResponseEntity<ApiResponse<AuthResponse>> response = authController.refresh(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("Token refreshed successfully");
        assertThat(response.getBody().getData().getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getBody().getData().getRefreshToken()).isEqualTo("new-refresh-token");

        verify(authService, times(1)).refreshToken(any(TokenRefreshRequest.class));
    }

    @Test
    @DisplayName("refresh() → TokenException fırlatmalı (token DB'de yok)")
    void refresh_shouldThrowTokenException_whenTokenNotFound() {
        TokenRefreshRequest request = new TokenRefreshRequest("nonexistent-token");

        when(authService.refreshToken(any(TokenRefreshRequest.class)))
                .thenThrow(new TokenException("Refresh token is not in database!"));

        assertThatThrownBy(() -> authController.refresh(request))
                .isInstanceOf(TokenException.class)
                .hasMessage("Refresh token is not in database!");
    }

    @Test
    @DisplayName("refresh() → TokenException fırlatmalı (token süresi dolmuş)")
    void refresh_shouldThrowTokenException_whenTokenIsExpired() {
        TokenRefreshRequest request = new TokenRefreshRequest("expired-token");

        when(authService.refreshToken(any(TokenRefreshRequest.class)))
                .thenThrow(new TokenException("Refresh token was expired. Please make a new signin request"));

        assertThatThrownBy(() -> authController.refresh(request))
                .isInstanceOf(TokenException.class)
                .hasMessage("Refresh token was expired. Please make a new signin request");
    }

    @Test
    @DisplayName("refresh() → yeni tokenlar eski tokenlardan farklı olmalı")
    void refresh_shouldReturnNewTokensDifferentFromOldOnes() {
        String oldRefreshToken = "old-refresh-token";
        TokenRefreshRequest request = new TokenRefreshRequest(oldRefreshToken);

        AuthResponse rotatedTokens = AuthResponse.builder()
                .accessToken("brand-new-access-token")
                .refreshToken("brand-new-refresh-token")
                .build();

        when(authService.refreshToken(request)).thenReturn(rotatedTokens);

        ResponseEntity<ApiResponse<AuthResponse>> response = authController.refresh(request);

        assertThat(response.getBody().getData().getRefreshToken())
                .isNotEqualTo(oldRefreshToken);
    }
}
