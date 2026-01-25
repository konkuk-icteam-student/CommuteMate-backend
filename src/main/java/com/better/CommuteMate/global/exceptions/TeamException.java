package com.better.CommuteMate.global.exceptions;

import com.better.CommuteMate.global.controller.dtos.ErrorResponseDetail;
import com.better.CommuteMate.global.exceptions.error.CustomErrorCode;
import lombok.Getter;

@Getter
public class TeamException extends BasicException {

  public TeamException(CustomErrorCode errorCode) {
    super(errorCode, errorCode.getLogMessage());
  }

  public TeamException(CustomErrorCode errorCode, ErrorResponseDetail detail) {
    super(errorCode, errorCode.getLogMessage(), detail);
  }
}