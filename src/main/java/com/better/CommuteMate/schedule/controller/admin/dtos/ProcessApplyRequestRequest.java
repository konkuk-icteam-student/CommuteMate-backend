package com.better.CommuteMate.schedule.controller.admin.dtos;

import com.better.CommuteMate.global.code.CodeType;
import java.util.List;

public record ProcessApplyRequestRequest(
        List<Integer> scheduleIds,
        CodeType statusCode // WS02 (승인) 또는 WS03 (거부)
) {}
