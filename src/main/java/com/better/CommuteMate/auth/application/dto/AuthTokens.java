package com.better.CommuteMate.auth.application.dto;

import com.better.CommuteMate.global.code.CodeType;

public class AuthTokens {
    private final String accessToken;
    private final String refreshToken;
    private final long expiresAt;
    private final Integer userId;
    private final String userName;
    private final String email;
    private final CodeType roleCode;

    public AuthTokens(String accessToken, String refreshToken, long expiresAt,
                      Integer userId, String userName, String email, CodeType roleCode) {
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

    public long getExpiresAt() {
        return expiresAt;
    }

    public Integer getUserId() {
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