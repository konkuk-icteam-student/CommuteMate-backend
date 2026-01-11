package com.better.CommuteMate.task.controller.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyTemplateRequest {

    @NotNull(message = "적용할 날짜는 필수입니다.")
    private LocalDate targetDate;

    private List<AssigneeOverride> assigneeOverrides; // 담당자 오버라이드 (선택)
}
