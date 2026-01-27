package com.better.CommuteMate.task.controller.dtos;

import com.better.CommuteMate.domain.task.entity.TaskTemplateItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateItemResponse {

    private Long itemId;
    private String title;
    private Long defaultAssigneeId;
    private String defaultAssigneeName;
    private LocalTime taskTime;
    private String taskType;
    private String taskTypeName;
    private Integer displayOrder;

    public static TemplateItemResponse from(TaskTemplateItem item) {
        return TemplateItemResponse.builder()
                .itemId(item.getItemId())
                .title(item.getTitle())
                .defaultAssigneeId(item.getDefaultAssignee() != null ? item.getDefaultAssignee().getUserId() : null)
                .defaultAssigneeName(item.getDefaultAssignee() != null ? item.getDefaultAssignee().getName() : null)
                .taskTime(item.getTaskTime())
                .taskType(item.getTaskType().getFullCode())
                .taskTypeName(item.getTaskType().getCodeValue())
                .displayOrder(item.getDisplayOrder())
                .build();
    }
}
