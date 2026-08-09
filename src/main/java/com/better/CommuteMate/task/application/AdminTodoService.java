package com.better.CommuteMate.task.application;

import com.better.CommuteMate.domain.task.entity.Task;
import com.better.CommuteMate.domain.task.repository.TaskRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.TaskErrorCode;
import com.better.CommuteMate.task.controller.dtos.AdminTodosResponse;
import com.better.CommuteMate.task.controller.dtos.CreateAdminTodoRequest;
import com.better.CommuteMate.task.controller.dtos.CreateAdminTodoResponse;
import com.better.CommuteMate.task.controller.dtos.UpdateAdminTodoRequest;
import com.better.CommuteMate.task.controller.dtos.UpdateAdminTodoResponse;
import com.better.CommuteMate.global.code.CodeType;
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

    @Transactional
    public CreateAdminTodoResponse createTodo(
            CreateAdminTodoRequest request,
            Long adminId
    ) {
        LocalDate date;
        LocalTime timeSlot;
        try {
            date = LocalDate.parse(request.date());
            timeSlot = LocalTime.parse(request.timeSlot());
        } catch (DateTimeParseException e) {
            throw CustomException.of(TaskErrorCode.INVALID_TODO_INFORMATION);
        }

        Task task = Task.create(
                request.description().trim(),
                date,
                timeSlot,
                CodeType.TT01,
                adminId
        );
        return CreateAdminTodoResponse.from(taskRepository.save(task));
    }

    @Transactional
    public UpdateAdminTodoResponse updateTodo(
            Long todoId,
            UpdateAdminTodoRequest request,
            Long adminId
    ) {
        Task task = taskRepository.findById(todoId)
                .orElseThrow(() -> CustomException.of(TaskErrorCode.TODO_NOT_FOUND));
        validateUpdateAccess(task, adminId);

        if (request.date() == null
                && request.timeSlot() == null
                && request.description() == null) {
            throw CustomException.of(TaskErrorCode.INVALID_TODO_INFORMATION);
        }

        LocalDate date = parseOptionalDate(request.date());
        LocalTime timeSlot = parseOptionalTime(request.timeSlot());
        String description = parseOptionalDescription(request.description());

        task.updateAdminTodo(date, timeSlot, description, adminId);
        return UpdateAdminTodoResponse.from(taskRepository.saveAndFlush(task));
    }

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

    private LocalDate parseOptionalDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw CustomException.of(TaskErrorCode.INVALID_TODO_INFORMATION);
        }
    }

    private LocalTime parseOptionalTime(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            throw CustomException.of(TaskErrorCode.INVALID_TODO_INFORMATION);
        }
    }

    private String parseOptionalDescription(String value) {
        if (value == null) {
            return null;
        }
        String description = value.trim();
        if (description.isEmpty()) {
            throw CustomException.of(TaskErrorCode.INVALID_TODO_INFORMATION);
        }
        return description;
    }

    private void validateUpdateAccess(Task task, Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> CustomException.of(TaskErrorCode.TODO_UPDATE_ACCESS_DENIED));
        User creator = task.getCreatedBy() == null
                ? null
                : userRepository.findById(task.getCreatedBy()).orElse(null);

        if (creator == null
                || !Objects.equals(admin.getOrganizationId(), creator.getOrganizationId())) {
            throw CustomException.of(TaskErrorCode.TODO_UPDATE_ACCESS_DENIED);
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
