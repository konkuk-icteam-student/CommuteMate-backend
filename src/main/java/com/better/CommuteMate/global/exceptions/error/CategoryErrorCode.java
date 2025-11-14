package com.better.CommuteMate.global.exceptions.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CategoryErrorCode implements CustomErrorCode {

    CATEGORY_NOT_FOUND("존재하지 않는 categoryId입니다.", "[Error] : Category를 찾을 수 없습니다."),
    CATEGORY_ALREADY_EXISTS("이미 존재하는 category 이름입니다.", "[Error] : Category 이름 중복 발생");

    private final String message;
    private final String logMessage;

    @Override
    public String getName() {
        return this.name();
    }
}