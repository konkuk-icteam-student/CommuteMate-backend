package com.better.CommuteMate.auth.application.dto;

import com.better.CommuteMate.global.code.CodeType;

public class AuthTokens {
    private final String accessToken;
    private final String refreshToken;
    private final Long expiresAt;
    private final Long userId;
    private final String userName;
    private final String email;
    private final CodeType roleCode;

    public AuthTokens(String accessToken, String refreshToken, Long expiresAt,
                      Long userId, String userName, String email, CodeType roleCode) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.userId = userId;
        this.userName = userName;
        this.email = email;
        this.roleCode = roleCode;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getEmail() {
        return email;
    }

    public CodeType getRoleCode() {
        return roleCode;
    }
}