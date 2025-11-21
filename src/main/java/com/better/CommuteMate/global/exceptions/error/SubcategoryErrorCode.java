package com.better.CommuteMate.global.exceptions.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SubcategoryErrorCode implements CustomErrorCode {

    SUBCATEGORY_NOT_FOUND("존재하지 않는 subcategoryId입니다.", "[Error] : SubCategory를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SUBCATEGORY_ALREADY_EXISTS("이미 존재하는 subcategory 이름입니다.", "[Error] : SubCategory 이름 중복 발생", HttpStatus.CONFLICT),
    SUBCATEGORY_DELETE_NOT_ALLOWED("해당 SubCategory에 속한 FAQ가 존재하여 삭제할 수 없습니다.", "[Error] : SubCategory 삭제 실패 - FAQ가 존재합니다.", HttpStatus.CONFLICT)
    ;

    private final String message;
    private final String logMessage;
    private final HttpStatus status;

    @Override
    public String getName() {
        return this.name();
    }
}