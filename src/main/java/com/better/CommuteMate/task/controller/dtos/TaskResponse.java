package com.better.CommuteMate.task.controller.dtos;

import com.better.CommuteMate.domain.task.entity.Task;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse extends ResponseDetail {

    private Long taskId;
    private String title;
    private Long assigneeId;
    private String assigneeName;
    private LocalDate taskDate;
    private LocalTime taskTime;
    private String taskType; // TT01 or TT02
    private String taskTypeName; // 정기 업무 or 비정기 업무
    private Boolean isCompleted;

    public static TaskResponse from(Task task) {
        return TaskResponse.builder()
                .taskId(task.getTaskId())
                .title(task.getTitle())
                .assigneeId(task.getAssignee().getUserId())
                .assigneeName(task.getAssignee().getName())
                .taskDate(task.getTaskDate())
                .taskTime(task.getTaskTime())
                .taskType(task.getTaskType().getFullCode())
                .taskTypeName(task.getTaskType().getCodeValue())
                .isCompleted(task.getIsCompleted())
                .build();
    }
}
