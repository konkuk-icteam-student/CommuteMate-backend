package com.better.CommuteMate.task.controller.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateItemRequest {

    @NotNull(message = "업무명은 필수입니다.")
    private String title;

    private Long defaultAssigneeId;

    @NotNull(message = "업무 시간은 필수입니다.")
    private LocalTime taskTime;

    private String taskType; // TT01 or TT02, 미입력 시 TT01

    private Integer displayOrder;
}
