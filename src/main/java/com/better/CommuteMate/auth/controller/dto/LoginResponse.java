package com.better.CommuteMate.controller.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {
    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final Long expiresAt;
}