package com.better.CommuteMate.task.controller.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateTodoCompletionRequest(
        @NotBlank(message = "날짜 형식이 올바르지 않습니다.")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "날짜 형식이 올바르지 않습니다.")
        String date,

        @NotNull(message = "완료 여부 값이 올바르지 않습니다.")
        Boolean isCompleted
) {
}
