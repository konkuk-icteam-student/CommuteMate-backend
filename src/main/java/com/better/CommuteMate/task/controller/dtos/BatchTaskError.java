package com.better.CommuteMate.task.controller.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchTaskError {

    private Long taskId;
    private String title;
    private String errorMessage;
}
