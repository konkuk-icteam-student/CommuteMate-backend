package com.better.CommuteMate.task.controller.dtos;

import com.better.CommuteMate.domain.task.entity.TaskTemplate;
import com.better.CommuteMate.domain.task.entity.TaskTemplateItem;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateDetailResponse extends ResponseDetail {

    private Long templateId;
    private String templateName;
    private String description;
    private Boolean isActive;
    private List<TemplateItemResponse> items;

    public static TemplateDetailResponse from(TaskTemplate template, List<TaskTemplateItem> items) {
        return TemplateDetailResponse.builder()
                .templateId(template.getTemplateId())
                .templateName(template.getTemplateName())
                .description(template.getDescription())
                .isActive(template.getIsActive())
                .items(items.stream()
                        .map(TemplateItemResponse::from)
                        .collect(Collectors.toList()))
                .build();
    }
}
