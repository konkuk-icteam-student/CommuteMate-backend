package com.better.CommuteMate.global.exceptions.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ManagerErrorCode implements CustomErrorCode {

    ALREADY_MANAGER("이미 관리자 권한을 가진 사용자입니다.", "[Error] : Manager 등록 실패 - 이미 관리자 권한 보유", HttpStatus.CONFLICT),
    MANAGER_CATEGORY_ALREADY_EXISTS("이미 등록된 manager-category 매핑입니다.", "[Error] : Manager-Category 중복 매핑 발생", HttpStatus.CONFLICT),
    MANAGER_ROLE_NOT_ASSIGNED("해당 사용자는 manager 권한이 없습니다.", "[Error] : Manager 권한이 아님", HttpStatus.BAD_REQUEST);

    private final String message;
    private final String logMessage;
    private final HttpStatus status;

    @Override
    public String getName() {
        return this.name();
    }
}