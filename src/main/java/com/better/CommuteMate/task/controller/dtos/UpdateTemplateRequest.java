package com.better.CommuteMate.task.controller.dtos;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTemplateRequest {

    @Size(max = 100, message = "템플릿 이름은 100자 이내로 입력해주세요.")
    private String templateName;

    @Size(max = 500, message = "설명은 500자 이내로 입력해주세요.")
    private String description;

    private List<TemplateItemRequest> items; // null이면 항목 변경 안함, 빈 리스트면 항목 모두 삭제
}
