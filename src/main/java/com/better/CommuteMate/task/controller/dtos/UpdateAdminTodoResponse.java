package com.better.CommuteMate.task.controller.dtos;

import com.better.CommuteMate.domain.todo.entity.Todo;
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

    private UpdateAdminTodoResponse(Todo todo) {
        this.todoId = todo.getTodoId();
        this.date = todo.getDate();
        this.timeSlot = todo.getTimeSlot();
        this.description = todo.getDescription();
        this.completed = Boolean.TRUE.equals(todo.getIsCompleted());
        this.status = completed ? "COMPLETED" : "PENDING";
        this.updatedAt = todo.getUpdatedAt();
    }

    public static UpdateAdminTodoResponse from(Todo todo) {
        return new UpdateAdminTodoResponse(todo);
    }

    @Override
    @JsonIgnore
    public LocalDateTime getTimestamp() {
        return super.getTimestamp();
    }
}
