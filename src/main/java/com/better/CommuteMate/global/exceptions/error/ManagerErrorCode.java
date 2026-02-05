package com.better.CommuteMate.global.exceptions.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ManagerErrorCode implements CustomErrorCode {

    MANAGER_CATEGORY_ALREADY_EXISTS("이미 등록된 manager-category 매핑입니다.", "[Error] : Manager-Category 중복 매핑 발생", HttpStatus.CONFLICT),
    MANAGER_NOT_FOUND("존재하지 않는 managerId입니다.", "[Error] : Manager를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    MANAGER_CATEGORY_NOT_FOUND("존재하지 않는 manager-category 매핑입니다.", "[Error] : Manager-Category를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    MANAGER_CATEGORY_MISMATCH("담당자와 카테고리가 일치하지 않습니다.", "[Error] : Manager-Category 매핑 불일치", HttpStatus.CONFLICT),
    MANAGER_DELETE_NOT_ALLOWED("해당 manager에 속한 카테고리가 존재하여 삭제할 수 없습니다.", "[Error] : Manager 삭제 실패 - 카테고리가 존재합니다.", HttpStatus.CONFLICT);
    ;

    private final String message;
    private final String logMessage;
    private final HttpStatus status;

    @Override
    public String getName() {
        return this.name();
    }
}