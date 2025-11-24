package com.better.CommuteMate.auth.controller.dto;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Builder;
import lombok.Getter;

/**
 * 로그인/토큰 갱신 응답 DTO
 */
@Getter
@Builder
public class LoginResponse extends ResponseDetail {
    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final Long expiresAt;
}