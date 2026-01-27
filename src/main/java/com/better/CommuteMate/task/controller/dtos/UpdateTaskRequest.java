package com.better.CommuteMate.task.controller.dtos;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskRequest {

    @Size(max = 200, message = "업무명은 200자 이내로 입력해주세요.")
    private String title;

    private Long assigneeId;

    private LocalTime taskTime;
}
