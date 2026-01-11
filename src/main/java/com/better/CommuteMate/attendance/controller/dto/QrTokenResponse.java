package com.better.CommuteMate.attendance.controller.dto;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class QrTokenResponse extends ResponseDetail {
    private String token;
    private LocalDateTime expiresAt;
    private int validSeconds;

    @Builder
    public QrTokenResponse(String token, LocalDateTime expiresAt, int validSeconds) {
        super();
        this.token = token;
        this.expiresAt = expiresAt;
        this.validSeconds = validSeconds;
    }
}
