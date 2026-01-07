package com.better.CommuteMate.task.controller.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
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
public class ApplyTemplateResponse extends ResponseDetail {

    private Long templateId;
    private String templateName;
    private LocalDate targetDate;
    private int createdCount;
    private List<TaskResponse> tasks;
}
