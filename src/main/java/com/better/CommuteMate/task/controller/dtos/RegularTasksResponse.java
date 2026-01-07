package com.better.CommuteMate.task.controller.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegularTasksResponse {

    private List<TaskResponse> morning;
    private List<TaskResponse> afternoon;
}
