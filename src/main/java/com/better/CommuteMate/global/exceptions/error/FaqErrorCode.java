package com.better.CommuteMate.global.exceptions.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FaqErrorCode implements CustomErrorCode {

    FAQ_NOT_FOUND("존재하지 않는 FaqId입니다.", "[Error] : Faq를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FAQ_ALREADY_DELETED("이미 삭제된 FAQ입니다.", "[Error] : 이미 삭제된 FAQ", HttpStatus.BAD_REQUEST),
    INVALID_FAQ_HISTORY_DATE("해당 날짜의 FAQ 수정 이력이 존재하지 않습니다.", "[Error] : FAQ 히스토리 날짜 조회 실패", HttpStatus.BAD_REQUEST),
    CATEGORY_LIMIT_EXCEEDED("지정 가능한 카테고리 개수를 초과했습니다.", "[Error] : 카테고리 개수 초과", HttpStatus.BAD_REQUEST),
    CATEGORY_REQUIRED("카테고리가 지정되지 않았습니다.", "[Error] : 카테고리 지정 누락", HttpStatus.BAD_REQUEST)
    ;

    private final String message;
    private final String logMessage;
    private final HttpStatus status;

    @Override
    public String getName() {
        return this.name();
    }
}