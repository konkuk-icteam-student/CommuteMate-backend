package com.better.CommuteMate.global.exceptions.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SubcategoryErrorCode implements CustomErrorCode {

    SUBCATEGORY_ALREADY_EXISTS("이미 존재하는 subcategory 이름입니다.", "[Error] : SubCategory 이름 중복 발생", HttpStatus.CONFLICT),
    ;

    private final String message;
    private final String logMessage;
    private final HttpStatus status;

    @Override
    public String getName() {
        return this.name();
    }
}