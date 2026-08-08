package com.better.CommuteMate.task.application;

import com.better.CommuteMate.domain.task.entity.Task;
import com.better.CommuteMate.domain.task.repository.TaskRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.TaskErrorCode;
import com.better.CommuteMate.task.controller.dtos.AdminTodosResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminTodoService {

    private static final LocalTime NOON = LocalTime.NOON;

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public AdminTodosResponse getTodos(Long organizationId, String dateValue) {
        LocalDate date = parseDate(dateValue);
        List<Task> tasks = taskRepository.findAdminTodos(organizationId, date);
        Map<Long, User> creators = findCreators(tasks);

        List<AdminTodosResponse.TodoItem> morningTodos = tasks.stream()
                .filter(task -> task.getTaskTime().isBefore(NOON))
                .map(task -> AdminTodosResponse.toItem(task, creators))
                .toList();
        List<AdminTodosResponse.TodoItem> afternoonTodos = tasks.stream()
                .filter(task -> !task.getTaskTime().isBefore(NOON))
                .map(task -> AdminTodosResponse.toItem(task, creators))
                .toList();

        return new AdminTodosResponse(date, morningTodos, afternoonTodos);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            throw CustomException.of(TaskErrorCode.INVALID_TODO_DATE);
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw CustomException.of(TaskErrorCode.INVALID_TODO_DATE);
        }
    }

    private Map<Long, User> findCreators(List<Task> tasks) {
        List<Long> creatorIds = tasks.stream()
                .map(Task::getCreatedBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (creatorIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(creatorIds).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));
    }
}
