package com.better.CommuteMate.task.controller.dtos;

import jakarta.validation.constraints.NotNull;

public record UpdateTodoCompletionRequest(
        @NotNull(message = "완료 여부 값이 올바르지 않습니다.")
        Boolean isCompleted
) {
}
