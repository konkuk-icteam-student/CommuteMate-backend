package com.better.CommuteMate.global.exceptions;

import com.better.CommuteMate.global.controller.dtos.ErrorResponseDetail;
import com.better.CommuteMate.global.exceptions.error.CustomErrorCode;
import lombok.Getter;

@Getter
public class FaqException extends BasicException {

  public FaqException(CustomErrorCode errorCode) {
    super(errorCode, errorCode.getLogMessage());
  }

  public FaqException(CustomErrorCode errorCode, ErrorResponseDetail detail) {
    super(errorCode, errorCode.getLogMessage(), detail);
  }
}