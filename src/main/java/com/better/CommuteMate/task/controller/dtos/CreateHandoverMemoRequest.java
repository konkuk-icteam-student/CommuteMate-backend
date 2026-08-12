package com.better.CommuteMate.task.controller.dtos;

import jakarta.validation.constraints.NotBlank;

public record CreateHandoverMemoRequest(
        @NotBlank(message = "메모 내용을 입력해야 합니다.")
        String content
) {
}
