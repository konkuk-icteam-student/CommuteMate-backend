package com.better.CommuteMate.attendance.controller.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CheckOutRequest {
    @jakarta.validation.constraints.NotBlank(message = "QR 토큰은 필수입니다.")
    private String qrToken;
}
