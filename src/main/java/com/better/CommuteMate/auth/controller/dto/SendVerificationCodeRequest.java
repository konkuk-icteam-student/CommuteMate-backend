package com.better.CommuteMate.auth.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 인증번호 발송 요청 DTO
 *
 * 사용처: POST /api/v1/auth/send-verification-code
 */
@Getter
@Setter
@NoArgsConstructor
public class SendVerificationCodeRequest {
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @NotBlank(message = "이메일은 필수입니다.")
    private String email;
}
