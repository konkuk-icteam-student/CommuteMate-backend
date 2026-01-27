package com.better.CommuteMate.attendance.controller.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CheckInRequest {
    private String qrToken;
}
