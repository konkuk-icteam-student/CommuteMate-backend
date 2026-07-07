package com.better.CommuteMate.global.exceptions.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StorageErrorCode implements CustomErrorCode {
    FILE_UPLOAD_FAILED("파일 업로드에 실패했습니다.", "[Error] 파일 업로드 실패", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_EMPTY("파일이 비어 있습니다.", "[Error] : 업로드된 파일이 비어 있음", HttpStatus.BAD_REQUEST),
    FILE_INVALID_NAME("파일 이름이 올바르지 않습니다.", "[Error] : 파일 이름 null 또는 확장자 없음", HttpStatus.BAD_REQUEST),
    FILE_UNSUPPORTED_TYPE("지원하지 않는 파일 형식입니다.", "[Error] : 허용되지 않은 파일 확장자", HttpStatus.BAD_REQUEST),
    ;

    private final String message;
    private final String logMessage;
    private final HttpStatus status;

    @Override
    public String getName() {
        return this.name();
    }
}
