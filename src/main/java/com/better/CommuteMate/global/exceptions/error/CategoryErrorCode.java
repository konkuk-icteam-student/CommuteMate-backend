package com.better.CommuteMate.global.exceptions.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CategoryErrorCode implements CustomErrorCode {

    CATEGORY_NOT_FOUND("존재하지 않는 categoryId입니다.", "[Error] : Category를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CATEGORY_ALREADY_EXISTS("이미 존재하는 category 이름입니다.", "[Error] : Category 이름 중복 발생", HttpStatus.CONFLICT),
    CATEGORY_DELETE_NOT_ALLOWED("해당 category에 속한 FAQ 또는 담당자가 존재하여 삭제할 수 없습니다.", "[Error] : Category 삭제 실패 - FAQ 또는 담당자가 존재합니다.", HttpStatus.CONFLICT);

    private final String message;
    private final String logMessage;
    private final HttpStatus status;

    @Override
    public String getName() {
        return this.name();
    }
}