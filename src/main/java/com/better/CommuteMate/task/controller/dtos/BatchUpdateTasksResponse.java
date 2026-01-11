package com.better.CommuteMate.task.controller.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUpdateTasksResponse extends ResponseDetail {

    private List<TaskResponse> createdTasks;
    private List<TaskResponse> updatedTasks;
    private List<BatchTaskError> errors;
    private int totalCreated;
    private int totalUpdated;
    private int totalErrors;
}
