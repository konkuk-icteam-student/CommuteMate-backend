package com.better.CommuteMate.auth.controller;

import com.better.CommuteMate.auth.controller.dto.RegisterRequest;
import com.better.CommuteMate.auth.controller.dto.RegisterResponse;
import com.better.CommuteMate.auth.controller.dto.SendVerificationCodeRequest;
import com.better.CommuteMate.auth.controller.dto.VerifyCodeRequest;
import com.better.CommuteMate.auth.controller.dto.LoginRequest;
import com.better.CommuteMate.auth.controller.dto.LoginResponse;
import com.better.CommuteMate.auth.application.AuthService;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.auth.application.dto.AuthTokens;
import com.better.CommuteMate.global.controller.dtos.Response;
import com.better.CommuteMate.global.exceptions.AuthException;
import com.better.CommuteMate.global.exceptions.error.AuthErrorCode;
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
    public ResponseEntity<Response> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        authService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok(
                new Response(true, "인증번호가 이메일로 발송되었습니다. (유효시간: 5분)", null)
        );
    }

    @PostMapping("/verify-code")
    public ResponseEntity<Response> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        authService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(
                new Response(true, "이메일 인증이 완료되었습니다. 회원가입을 진행해주세요.", null)
        );
    }

    @PostMapping("/register")
    public ResponseEntity<Response> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new Response(true, "회원가입이 완료되었습니다.", RegisterResponse.from(user))
        );
    }

    @PostMapping("/login")
    public ResponseEntity<Response> login(@Valid @RequestBody LoginRequest request) {
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
        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .tokenType("Bearer")
                .expiresAt(tokens.getExpiresAt())
                .userId(tokens.getUserId())
                .userName(tokens.getUserName())
                .email(tokens.getEmail())
                .roleCode(tokens.getRoleCode().getFullCode())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new Response(true, "로그인 성공", loginResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<Response> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            authService.logout(token);
        }
        return ResponseEntity.ok(
                new Response(true, "로그아웃되었습니다.", null)
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<Response> refresh(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new AuthException(AuthErrorCode.AUTHORIZATION_HEADER_MISSING);
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
        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .tokenType("Bearer")
                .expiresAt(tokens.getExpiresAt())
                .userId(tokens.getUserId())
                .userName(tokens.getUserName())
                .email(tokens.getEmail())
                .roleCode(tokens.getRoleCode().getFullCode())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new Response(true, "토큰 갱신 성공", loginResponse));
    }
}
