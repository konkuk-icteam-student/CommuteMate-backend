package com.better.CommuteMate.auth.application.dto;

public class AuthTokens {
    private final String accessToken;
    private final String refreshToken;
    private final long expiresAt;

    public AuthTokens(String accessToken, String refreshToken, long expiresAt) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
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
}