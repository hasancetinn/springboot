package com.ecommerce.demo.service;

import com.ecommerce.demo.dto.request.LoginRequest;
import com.ecommerce.demo.dto.request.RegisterRequest;
import com.ecommerce.demo.dto.request.TokenRefreshRequest;
import com.ecommerce.demo.dto.response.AuthResponse;
import com.ecommerce.demo.exception.ApiException;
import com.ecommerce.demo.exception.TokenException;
import com.ecommerce.demo.model.RefreshToken;
import com.ecommerce.demo.model.Role;
import com.ecommerce.demo.model.User;
import com.ecommerce.demo.repository.RefreshTokenRepository;
import com.ecommerce.demo.repository.UserRepository;
import com.ecommerce.demo.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * AuthService için saf unit testleri.
 *
 * @ExtendWith(MockitoExtension.class): Spring context yüklemez, sadece Mockito kullanır.
 * Tüm bağımlılıklar (@Mock) ile mock'lanır, @InjectMocks ile AuthService'e inject edilir.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    // @Value alanı için ReflectionTestUtils kullanıyoruz (7 gün = ms cinsinden)
    private static final long REFRESH_EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L;

    @BeforeEach
    void setUp() {
        // @Value("${application.security.jwt.refresh-token.expiration}") → manuel inject
        ReflectionTestUtils.setField(authService, "refreshExpiration", REFRESH_EXPIRATION_MS);
    }

    // ----------------------------------------------------------------
    // Yardımcı: test User oluştur
    // ----------------------------------------------------------------
    private User buildUser(String username, String email) {
        return User.builder()
                .username(username)
                .email(email)
                .password("encoded-password")
                .roles(Set.of(Role.ROLE_USER))
                .build();
    }

    private RefreshToken buildRefreshToken(User user, boolean expired) {
        Instant expiry = expired
                ? Instant.now().minusSeconds(3600)    // Süresi dolmuş
                : Instant.now().plusMillis(REFRESH_EXPIRATION_MS);
        return RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(expiry)
                .build();
    }

    // ================================================================
    // register() testleri
    // ================================================================

    @Test
    @DisplayName("register() → AuthResponse döndürmeli (başarılı kayıt)")
    void register_shouldReturnAuthResponse_whenRequestIsValid() {
        // Hazırlık
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("password123")
                .build();

        User savedUser = buildUser("newuser", "new@example.com");
        RefreshToken refreshToken = buildRefreshToken(savedUser, false);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("mock-access-token");
        doNothing().when(refreshTokenRepository).deleteByUser(any(User.class));
        doNothing().when(refreshTokenRepository).flush();
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        // Çalıştır
        AuthResponse response = authService.register(request);

        // Doğrula
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock-access-token");
        assertThat(response.getRefreshToken()).isEqualTo(refreshToken.getToken());

        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).existsByEmail("new@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken(any(User.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("register() → ApiException fırlatmalı (kullanıcı adı zaten alınmış)")
    void register_shouldThrowApiException_whenUsernameAlreadyTaken() {
        RegisterRequest request = RegisterRequest.builder()
                .username("existinguser")
                .email("new@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // Çalıştır & Doğrula
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .hasMessage("Username is already taken");

        verify(userRepository).existsByUsername("existinguser");
        // Email kontrolüne asla gidilmemeli
        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register() → ApiException fırlatmalı (email zaten kullanımda)")
    void register_shouldThrowApiException_whenEmailAlreadyInUse() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("taken@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiException.class)
                .hasMessage("Email is already in use");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register() → şifreyi hash'leyerek kaydetmeli (ham şifre DB'ye gitmemeli)")
    void register_shouldEncodePasswordBeforeSaving() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("plaintext-password")
                .build();

        User savedUser = buildUser("newuser", "new@example.com");
        RefreshToken refreshToken = buildRefreshToken(savedUser, false);

        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode("plaintext-password")).thenReturn("$2a$10$hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any())).thenReturn("token");
        doNothing().when(refreshTokenRepository).deleteByUser(any(User.class));
        doNothing().when(refreshTokenRepository).flush();
        when(refreshTokenRepository.save(any())).thenReturn(refreshToken);

        authService.register(request);

        // Kaydedilen User'ın şifresinin encode edilmiş olduğunu doğrula
        verify(userRepository).save(argThat(user ->
                user.getPassword().equals("$2a$10$hashedpassword")
        ));
    }

    // ================================================================
    // login() testleri
    // ================================================================

    @Test
    @DisplayName("login() → AuthResponse döndürmeli (başarılı giriş)")
    void login_shouldReturnAuthResponse_whenCredentialsAreValid() {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        User user = buildUser("testuser", "test@example.com");
        RefreshToken refreshToken = buildRefreshToken(user, false);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null); // Authentication başarılı
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("access-token");
        doNothing().when(refreshTokenRepository).deleteByUser(any(User.class));
        doNothing().when(refreshTokenRepository).flush();
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo(refreshToken.getToken());

        verify(authenticationManager).authenticate(
                argThat(auth ->
                        auth.getPrincipal().equals("testuser") &&
                        auth.getCredentials().equals("password123")
                )
        );
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("login() → BadCredentialsException fırlatmalı (yanlış şifre)")
    void login_shouldThrowBadCredentials_whenPasswordIsWrong() {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("wrongpassword")
                .build();

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("login() → ApiException fırlatmalı (kimlik doğrulandı ama kullanıcı DB'de yok – edge case)")
    void login_shouldThrowApiException_whenUserNotFoundAfterAuthentication() {
        LoginRequest request = LoginRequest.builder()
                .username("ghostuser")
                .password("password123")
                .build();

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByUsername("ghostuser")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ApiException.class)
                .hasMessage("User not found");
    }

    // ================================================================
    // refreshToken() testleri
    // ================================================================

    @Test
    @DisplayName("refreshToken() → yeni AuthResponse döndürmeli (token rotation)")
    void refreshToken_shouldReturnNewTokens_whenRefreshTokenIsValid() {
        User user = buildUser("testuser", "test@example.com");
        String oldTokenStr = "old-refresh-token";
        RefreshToken oldToken = RefreshToken.builder()
                .token(oldTokenStr)
                .user(user)
                .expiryDate(Instant.now().plusMillis(REFRESH_EXPIRATION_MS))
                .build();

        RefreshToken newRefreshToken = buildRefreshToken(user, false);

        TokenRefreshRequest request = new TokenRefreshRequest(oldTokenStr);

        when(refreshTokenRepository.findByToken(oldTokenStr)).thenReturn(Optional.of(oldToken));
        doNothing().when(refreshTokenRepository).delete(oldToken);
        doNothing().when(refreshTokenRepository).flush();
        when(jwtService.generateToken(user)).thenReturn("new-access-token");
        doNothing().when(refreshTokenRepository).deleteByUser(any(User.class));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(newRefreshToken);

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo(newRefreshToken.getToken());

        // Eski token silinmeli (token rotation)
        verify(refreshTokenRepository).delete(oldToken);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("refreshToken() → TokenException fırlatmalı (token DB'de yok)")
    void refreshToken_shouldThrowTokenException_whenTokenNotInDatabase() {
        TokenRefreshRequest request = new TokenRefreshRequest("nonexistent-token");

        when(refreshTokenRepository.findByToken("nonexistent-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(TokenException.class)
                .hasMessage("Refresh token is not in database!");

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("refreshToken() → TokenException fırlatmalı (token süresi dolmuş)")
    void refreshToken_shouldThrowTokenException_whenTokenIsExpired() {
        User user = buildUser("testuser", "test@example.com");
        String expiredTokenStr = "expired-refresh-token";
        RefreshToken expiredToken = buildRefreshToken(user, true); // expired = true
        expiredToken = RefreshToken.builder()
                .token(expiredTokenStr)
                .user(user)
                .expiryDate(Instant.now().minusSeconds(3600)) // 1 saat önce dolmuş
                .build();

        TokenRefreshRequest request = new TokenRefreshRequest(expiredTokenStr);

        when(refreshTokenRepository.findByToken(expiredTokenStr)).thenReturn(Optional.of(expiredToken));
        // verifyExpiration metodunda delete çağrılıyor
        doNothing().when(refreshTokenRepository).delete(any(RefreshToken.class));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(TokenException.class)
                .hasMessage("Refresh token was expired. Please make a new signin request");

        // Süresi dolmuş token silinmeli
        verify(refreshTokenRepository).delete(any(RefreshToken.class));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("refreshToken() → eski token silinmeli (token rotation kontrolü)")
    void refreshToken_shouldDeleteOldToken_beforeCreatingNewOne() {
        User user = buildUser("testuser", "test@example.com");
        String tokenStr = "valid-refresh-token";
        RefreshToken validToken = RefreshToken.builder()
                .token(tokenStr)
                .user(user)
                .expiryDate(Instant.now().plusMillis(REFRESH_EXPIRATION_MS))
                .build();

        RefreshToken newToken = buildRefreshToken(user, false);

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(validToken));
        doNothing().when(refreshTokenRepository).delete(validToken);
        doNothing().when(refreshTokenRepository).flush();
        when(jwtService.generateToken(user)).thenReturn("new-access-token");
        doNothing().when(refreshTokenRepository).deleteByUser(any(User.class));
        when(refreshTokenRepository.save(any())).thenReturn(newToken);

        authService.refreshToken(new TokenRefreshRequest(tokenStr));

        // Token rotation: önce eski token silinmeli, sonra yeni kaydedilmeli
        verify(refreshTokenRepository).delete(validToken);
        verify(refreshTokenRepository).save(any(RefreshToken.class));

        // Sıra önemli: delete → flush → save
        var inOrder = inOrder(refreshTokenRepository);
        inOrder.verify(refreshTokenRepository).delete(validToken);
        inOrder.verify(refreshTokenRepository).flush();
        inOrder.verify(refreshTokenRepository).save(any(RefreshToken.class));
    }
}
