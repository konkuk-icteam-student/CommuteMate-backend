package com.better.CommuteMate.application.auth;

import com.better.CommuteMate.controller.auth.dto.RegisterRequest;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.security.jwt.JwtTokenProvider;
import com.better.CommuteMate.application.auth.TokenBlacklistService;
import com.better.CommuteMate.application.auth.dto.AuthTokens;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email already registered");
        }
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User user = User.create(
                request.getEmail(),
                hashedPassword,
                request.getName(),
                request.getOrganizationId(),
                request.getRoleCode()
        );
        return userRepository.save(user);
    }

    @Transactional
    public AuthTokens login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRoleCode());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail(), user.getRoleCode());
        // refresh token 저장
        user.setRefreshToken(refreshToken);
        userRepository.save(user);
        long expiresAt = jwtTokenProvider.getExpiration(accessToken);
        return new AuthTokens(accessToken, refreshToken, expiresAt);
    }

    public void logout(String token) {
        if (token != null && !token.isEmpty()) {
            // access token 블랙리스트에 추가
            tokenBlacklistService.blacklist(token);
            try {
                String email = jwtTokenProvider.getEmail(token);
                userRepository.findByEmail(email).ifPresent(user -> {
                    user.setRefreshToken(null);
                    userRepository.save(user);
                });
            } catch (Exception ignored) {
            }
        }
    }

    @Transactional
    public AuthTokens refresh(String refreshToken) {
        jwtTokenProvider.validateToken(refreshToken);
        String email = jwtTokenProvider.getEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String stored = user.getRefreshToken();
        if (stored == null || !stored.equals(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRoleCode());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getEmail(), user.getRoleCode());
        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);
        long expiresAt = jwtTokenProvider.getExpiration(newAccessToken);
        return new AuthTokens(newAccessToken, newRefreshToken, expiresAt);
    }
}
