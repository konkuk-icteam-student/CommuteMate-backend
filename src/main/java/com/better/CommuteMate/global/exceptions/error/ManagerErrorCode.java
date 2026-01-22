package com.better.CommuteMate.global.exceptions.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ManagerErrorCode implements CustomErrorCode {

    MANAGER_CATEGORY_ALREADY_EXISTS("이미 등록된 manager-category 매핑입니다.", "[Error] : Manager-Category 중복 매핑 발생", HttpStatus.CONFLICT);

    private final String message;
    private final String logMessage;
    private final HttpStatus status;

    @Override
    public String getName() {
        return this.name();
    }
}