package com.better.CommuteMate.task.controller.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchTaskItem {

    private Long taskId; // null이면 새로 생성, 있으면 수정

    private String title; // 새로 생성 시 필수

    private Integer assigneeId;

    private LocalTime taskTime;

    private String taskType; // TT01 or TT02

    private Boolean isCompleted;
}
