package com.better.CommuteMate.task.controller.dtos;

import com.better.CommuteMate.domain.task.entity.Task;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class UpdateAdminTodoResponse extends ResponseDetail {
    public final Long todoId;
    public final LocalDate date;
    public final LocalTime timeSlot;
    public final String description;
    public final String status;
    public final boolean completed;
    public final LocalDateTime updatedAt;

    private UpdateAdminTodoResponse(Task task) {
        this.todoId = task.getTaskId();
        this.date = task.getTaskDate();
        this.timeSlot = task.getTaskTime();
        this.description = task.getTitle();
        this.completed = Boolean.TRUE.equals(task.getIsCompleted());
        this.status = completed ? "COMPLETED" : "PENDING";
        this.updatedAt = task.getUpdatedAt();
    }

    public static UpdateAdminTodoResponse from(Task task) {
        return new UpdateAdminTodoResponse(task);
    }

    @Override
    @JsonIgnore
    public LocalDateTime getTimestamp() {
        return super.getTimestamp();
    }
}
