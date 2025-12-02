package com.better.CommuteMate.schedule.controller.admin.dtos;

import com.better.CommuteMate.global.code.CodeType;

public record ProcessChangeRequestRequest(
        Integer requestId,
        CodeType statusCode
) {
}
