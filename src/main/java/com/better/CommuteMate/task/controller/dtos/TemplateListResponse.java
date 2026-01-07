package com.better.CommuteMate.task.controller.dtos;

import com.better.CommuteMate.domain.task.entity.TaskTemplate;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateListResponse extends ResponseDetail {

    private Long templateId;
    private String templateName;
    private String description;
    private Boolean isActive;
    private int itemCount;

    public static TemplateListResponse from(TaskTemplate template) {
        return TemplateListResponse.builder()
                .templateId(template.getTemplateId())
                .templateName(template.getTemplateName())
                .description(template.getDescription())
                .isActive(template.getIsActive())
                .itemCount(template.getItems() != null ? template.getItems().size() : 0)
                .build();
    }
}
