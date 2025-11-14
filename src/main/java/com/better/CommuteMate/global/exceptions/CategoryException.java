package com.better.CommuteMate.global.exceptions;

import com.better.CommuteMate.global.controller.dtos.ErrorResponseDetail;
import com.better.CommuteMate.global.exceptions.error.CustomErrorCode;
import lombok.Getter;

@Getter
public class CategoryException extends BasicException {

    public CategoryException(CustomErrorCode errorCode) {
        super(errorCode, errorCode.getLogMessage());
    }

    public CategoryException(CustomErrorCode errorCode, ErrorResponseDetail detail) {
        super(errorCode, errorCode.getLogMessage(), detail);
    }
}