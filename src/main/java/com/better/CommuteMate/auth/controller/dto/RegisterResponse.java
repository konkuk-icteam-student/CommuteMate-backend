package com.better.CommuteMate.auth.controller.dto;

import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Builder;
import lombok.Getter;

/**
 * 회원가입 응답 DTO
 * User 엔티티를 직접 노출하지 않고 필요한 정보만 반환
 */
@Getter
@Builder
public class RegisterResponse extends ResponseDetail {
    private final Long userId;
    private final String email;
    private final String name;
    private final String roleCode;

    public static RegisterResponse from(User user) {
        return RegisterResponse.builder()
                .userId(user.getUserId().longValue())
                .email(user.getEmail())
                .name(user.getName())
                .roleCode(user.getRoleCode().getCodeValue())
                .build();
    }
}
