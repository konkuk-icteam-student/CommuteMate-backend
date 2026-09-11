package com.better.CommuteMate.task.controller.dtos;

import com.better.CommuteMate.domain.todo.entity.Todo;
import com.better.CommuteMate.domain.todo.entity.TodoCompletion;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public class AdminTodosResponse extends ResponseDetail {
    public final LocalDate date;
    public final List<TodoItem> morningTodos;
    public final List<TodoItem> afternoonTodos;

    public AdminTodosResponse(LocalDate date, List<TodoItem> morningTodos, List<TodoItem> afternoonTodos) {
        this.date = date;
        this.morningTodos = morningTodos;
        this.afternoonTodos = afternoonTodos;
    }

    public static TodoItem toItem(
            Todo todo,
            TodoCompletion completion,
            Map<Long, User> creators
    ) {
        User creator = creators.get(todo.getCreatedBy());
        return new TodoItem(
                todo.getTodoId(),
                todo.getDescription(),
                todo.getTimeSlot(),
                completion != null ? "COMPLETED" : "PENDING",
                creator == null ? null : new CreatedBy(creator.getUserId(), creator.getName()),
                todo.getCreatedAt(),
                completion != null ? completion.getCompletedByName() : null,
                completion != null ? completion.getCompletedTime() : null
        );
    }

    @Override
    @JsonIgnore
    public LocalDateTime getTimestamp() {
        return super.getTimestamp();
    }

    public record TodoItem(
            Long todoId,
            String description,
            LocalTime timeSlot,
            String status,
            CreatedBy createdBy,
            LocalDateTime createdAt,
            String completedByName,
            LocalTime completedTime
    ) {
    }

    public record CreatedBy(Long userId, String name) {
    }
}
