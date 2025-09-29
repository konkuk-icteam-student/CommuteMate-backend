package com.better.CommuteMate.controller.auth;

import com.better.CommuteMate.controller.auth.dto.RegisterRequest;
import com.better.CommuteMate.controller.auth.dto.LoginRequest;
import com.better.CommuteMate.controller.auth.dto.LoginResponse;
import com.better.CommuteMate.application.auth.AuthService;
import com.better.CommuteMate.domain.user.entity.UserEntity;
import com.better.CommuteMate.application.auth.dto.AuthTokens;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserEntity> register(@Valid @RequestBody RegisterRequest request) {
        UserEntity user = authService.register(request);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthTokens tokens = authService.login(request.getEmail(), request.getPassword());
        // HttpOnly cookie에 access token 주입
        long maxAgeSeconds = Math.max((tokens.getExpiresAt() - System.currentTimeMillis()) / 1000, 0);
        ResponseCookie cookie = ResponseCookie.from("accessToken", tokens.getAccessToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite("Strict")
                .build();
        LoginResponse response = LoginResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .tokenType("Bearer")
                .expiresAt(tokens.getExpiresAt())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            authService.logout(token);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String refreshToken = header.substring(7);
        try {
            AuthTokens tokens = authService.refresh(refreshToken);
            long maxAgeSeconds = Math.max((tokens.getExpiresAt() - System.currentTimeMillis()) / 1000, 0);
            ResponseCookie cookie = ResponseCookie.from("accessToken", tokens.getAccessToken())
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(maxAgeSeconds)
                    .sameSite("Strict")
                    .build();
            LoginResponse response = LoginResponse.builder()
                    .accessToken(tokens.getAccessToken())
                    .refreshToken(tokens.getRefreshToken())
                    .tokenType("Bearer")
                    .expiresAt(tokens.getExpiresAt())
                    .build();
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
