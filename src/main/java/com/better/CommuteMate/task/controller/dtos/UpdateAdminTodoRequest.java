package com.better.CommuteMate.task.controller.dtos;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAdminTodoRequest(
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "업무사항 입력값이 올바르지 않습니다.")
        String date,

        @Pattern(regexp = "(?:[01]\\d|2[0-3]):[0-5]\\d", message = "업무사항 입력값이 올바르지 않습니다.")
        String timeSlot,

        @Size(max = 200, message = "업무사항 입력값이 올바르지 않습니다.")
        String description
) {
}
