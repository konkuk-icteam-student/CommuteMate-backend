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
public class DailyTasksResponse extends ResponseDetail {

    private LocalDate date;
    private RegularTasksResponse regularTasks;
    private List<TaskResponse> irregularTasks;
}
