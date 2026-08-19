package com.better.CommuteMate.task.controller.dtos;

import com.better.CommuteMate.domain.todo.entity.Todo;
import com.better.CommuteMate.domain.todo.entity.TodoCompletion;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class UpdateTodoCompletionResponse extends ResponseDetail {
    public final LocalDate date;
    public final TodoDetail todo;
    public final Summary summary;

    private UpdateTodoCompletionResponse(LocalDate date, TodoDetail todo, Summary summary) {
        this.date = date;
        this.todo = todo;
        this.summary = summary;
    }

    public static UpdateTodoCompletionResponse of(
            Todo todo,
            LocalDate date,
            TodoCompletion completion,
            int completedCount,
            int totalCount
    ) {
        boolean completed = completion != null;
        TodoDetail detail = new TodoDetail(
                todo.getTodoId(),
                todo.getDescription(),
                todo.getTimeSlot(),
                completed ? "COMPLETED" : "PENDING",
                completed ? completion.getCompletedByName() : null,
                completed ? completion.getCompletedTime() : null
        );
        return new UpdateTodoCompletionResponse(
                date,
                detail,
                new Summary(completedCount, totalCount)
        );
    }

    @Override
    @JsonIgnore
    public LocalDateTime getTimestamp() {
        return super.getTimestamp();
    }

    public record TodoDetail(
            Long todoId,
            String description,
            LocalTime timeSlot,
            String status,
            String completedByName,
            LocalTime completedTime
    ) {
    }

    public record Summary(int completedCount, int totalCount) {
    }
}
