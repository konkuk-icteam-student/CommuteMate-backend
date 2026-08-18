package com.better.CommuteMate.task.controller.dtos;

import com.better.CommuteMate.domain.todo.entity.Todo;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class CreateAdminTodoResponse extends ResponseDetail {
    public final Long todoId;
    public final LocalTime timeSlot;
    public final String description;
    public final LocalDateTime createdAt;

    private CreateAdminTodoResponse(Todo todo) {
        this.todoId = todo.getTodoId();
        this.timeSlot = todo.getTimeSlot();
        this.description = todo.getDescription();
        this.createdAt = todo.getCreatedAt();
    }

    public static CreateAdminTodoResponse from(Todo todo) {
        return new CreateAdminTodoResponse(todo);
    }

    @Override
    @JsonIgnore
    public LocalDateTime getTimestamp() {
        return super.getTimestamp();
    }
}
