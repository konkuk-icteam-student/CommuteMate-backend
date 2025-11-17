package com.better.CommuteMate.auth.controller;

import com.better.CommuteMate.auth.application.AuthErrorCode;
import com.better.CommuteMate.auth.controller.dto.RegisterRequest;
import com.better.CommuteMate.auth.controller.dto.SendVerificationCodeRequest;
import com.better.CommuteMate.auth.controller.dto.VerifyCodeRequest;
import com.better.CommuteMate.auth.controller.dto.LoginRequest;
import com.better.CommuteMate.auth.controller.dto.LoginResponse;
import com.better.CommuteMate.auth.application.AuthService;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.auth.application.dto.AuthTokens;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/auth")
public class AuthController {

    private final AuthService authService;
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/send-verification-code")
    public ResponseEntity<String> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        authService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok("인증번호가 이메일로 발송되었습니다. (유효시간: 5분)");
    }

    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        authService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok("이메일 인증이 완료되었습니다. 회원가입을 진행해주세요.");
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
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
            throw new IllegalArgumentException(AuthErrorCode.AUTHORIZATION_HEADER_MISSING.getMessage());
        }
        String refreshToken = header.substring(7);
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
    }
}
