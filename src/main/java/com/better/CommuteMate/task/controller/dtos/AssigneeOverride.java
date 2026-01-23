package com.better.CommuteMate.task.controller.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssigneeOverride {

    private Long itemId; // 템플릿 항목 ID
    private Long assigneeId; // 오버라이드할 담당자 ID
}
