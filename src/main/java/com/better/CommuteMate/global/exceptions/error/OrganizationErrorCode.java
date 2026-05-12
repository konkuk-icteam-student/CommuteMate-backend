package com.better.CommuteMate.global.exceptions.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrganizationErrorCode implements CustomErrorCode {

    ORGANIZATION_NOT_FOUND("존재하지 않는 organizationId입니다.", "[Error] : organization을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ORGANIZATION_ALREADY_EXISTS("이미 존재하는 organization 이름입니다.", "[Error] : organization 이름 중복 발생", HttpStatus.CONFLICT),
    ORGANIZATION_DELETE_NOT_ALLOWED("해당 organization에 속한 담당자가 존재하여 삭제할 수 없습니다.", "[Error] : organization 삭제 실패 - 담당자가 존재합니다.", HttpStatus.CONFLICT)
    ;

    private final String message;
    private final String logMessage;
    private final HttpStatus status;

    @Override
    public String getName() {
        return this.name();
    }
}