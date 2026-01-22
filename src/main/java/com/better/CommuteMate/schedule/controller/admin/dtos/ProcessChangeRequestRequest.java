package com.better.CommuteMate.schedule.controller.admin.dtos;

import com.better.CommuteMate.global.code.CodeType;

import java.util.List;

public record ProcessChangeRequestRequest(
        List<Long> requestIds,
        CodeType statusCode
) {
}
