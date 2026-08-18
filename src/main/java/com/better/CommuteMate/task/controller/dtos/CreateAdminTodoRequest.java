package com.better.CommuteMate.task.controller.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAdminTodoRequest(
        @NotBlank(message = "업무사항 입력값이 올바르지 않습니다.")
        @Pattern(regexp = "(?:[01]\\d|2[0-3]):[0-5]\\d", message = "업무사항 입력값이 올바르지 않습니다.")
        String timeSlot,

        @NotBlank(message = "업무사항 입력값이 올바르지 않습니다.")
        @Size(max = 200, message = "업무사항 입력값이 올바르지 않습니다.")
        String description
) {
}
