package com.better.CommuteMate.global.exceptions.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FaqErrorCode implements CustomErrorCode {

    FAQ_NOT_FOUND("존재하지 않는 FaqId입니다.", "[Error] : Faq를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FAQ_ALREADY_DELETED("삭제된 FAQ는 수정할 수 없습니다.", "[Error] : Faq 수정 실패", HttpStatus.BAD_REQUEST);

    private final String message;
    private final String logMessage;
    private final HttpStatus status;

    @Override
    public String getName() {
        return this.name();
    }
}