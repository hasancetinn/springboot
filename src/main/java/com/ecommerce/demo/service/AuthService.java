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
import com.ecommerce.demo.dto.event.EmailEvent;
import com.ecommerce.demo.service.messaging.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final KafkaProducerService kafkaProducerService;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ApiException("Username is already taken", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("Email is already in use", HttpStatus.CONFLICT);
        }

        var user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(Role.ROLE_USER))
                .build();
        userRepository.save(user);

        var jwtToken = jwtService.generateToken(user);
        var refreshToken = createRefreshToken(user);

        // Produce Welcome Email Event
        kafkaProducerService.sendEmailEvent(EmailEvent.builder()
                .to(user.getEmail())
                .subject("Welcome to E-Commerce Demo!")
                .body("Hello " + user.getUsername() + ", welcome to our platform!")
                .build());

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken.getToken())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        var jwtToken = jwtService.generateToken(user);
        var refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken.getToken())
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenRepository.findByToken(requestRefreshToken)
                .map(this::verifyExpiration)
                .orElseThrow(() -> new TokenException("Refresh token is not in database!"));

        User user = refreshToken.getUser();
        
        // Token Rotation: Invalidate used token and generate new one
        refreshTokenRepository.delete(refreshToken);
        refreshTokenRepository.flush();

        String newAccessToken = jwtService.generateToken(user);
        RefreshToken newRefreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .build();
    }

    private RefreshToken createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user); // Clear old tokens
        refreshTokenRepository.flush();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshExpiration))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    private RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }
}
