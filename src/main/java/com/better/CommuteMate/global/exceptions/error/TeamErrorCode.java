package com.better.CommuteMate.global.exceptions.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TeamErrorCode implements CustomErrorCode {

    TEAM_NOT_FOUND("존재하지 않는 TeamId입니다.", "[Error] : Team을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String message;
    private final String logMessage;
    private final HttpStatus status;

    @Override
    public String getName() {
        return this.name();
    }
}