package com.better.CommuteMate.task.application;

import com.better.CommuteMate.domain.todo.entity.Todo;
import com.better.CommuteMate.domain.todo.repository.TodoRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.TodoErrorCode;
import com.better.CommuteMate.task.controller.dtos.AdminTodosResponse;
import com.better.CommuteMate.task.controller.dtos.CreateAdminTodoRequest;
import com.better.CommuteMate.task.controller.dtos.CreateAdminTodoResponse;
import com.better.CommuteMate.task.controller.dtos.UpdateAdminTodoRequest;
import com.better.CommuteMate.task.controller.dtos.UpdateAdminTodoResponse;
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

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreateAdminTodoResponse createTodo(
            CreateAdminTodoRequest request,
            Long adminId,
            Long organizationId
    ) {
        LocalDate date;
        LocalTime timeSlot;
        try {
            date = LocalDate.parse(request.date());
            timeSlot = LocalTime.parse(request.timeSlot());
        } catch (DateTimeParseException e) {
            throw CustomException.of(TodoErrorCode.INVALID_TODO_INFORMATION);
        }

        Todo todo = Todo.create(
                organizationId,
                request.description().trim(),
                date,
                timeSlot,
                adminId
        );
        return CreateAdminTodoResponse.from(todoRepository.save(todo));
    }

    @Transactional
    public UpdateAdminTodoResponse updateTodo(
            Long todoId,
            UpdateAdminTodoRequest request,
            Long adminId,
            Long organizationId
    ) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> CustomException.of(TodoErrorCode.TODO_NOT_FOUND));
        validateUpdateAccess(todo, organizationId);

        if (request.date() == null
                && request.timeSlot() == null
                && request.description() == null) {
            throw CustomException.of(TodoErrorCode.INVALID_TODO_INFORMATION);
        }

        LocalDate date = parseOptionalDate(request.date());
        LocalTime timeSlot = parseOptionalTime(request.timeSlot());
        String description = parseOptionalDescription(request.description());

        todo.update(date, timeSlot, description, adminId);
        return UpdateAdminTodoResponse.from(todoRepository.saveAndFlush(todo));
    }

    @Transactional
    public void deleteTodo(Long todoId, Long adminId, Long organizationId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> CustomException.of(TodoErrorCode.TODO_NOT_FOUND));
        validateDeleteAccess(todo, organizationId);
        todoRepository.delete(todo);
    }

    public AdminTodosResponse getTodos(Long organizationId, String dateValue) {
        LocalDate date = parseDate(dateValue);
        List<Todo> todos = todoRepository.findByOrganizationIdAndDate(organizationId, date);
        Map<Long, User> creators = findCreators(todos);

        List<AdminTodosResponse.TodoItem> morningTodos = todos.stream()
                .filter(todo -> todo.getTimeSlot().isBefore(NOON))
                .map(todo -> AdminTodosResponse.toItem(todo, creators))
                .toList();
        List<AdminTodosResponse.TodoItem> afternoonTodos = todos.stream()
                .filter(todo -> !todo.getTimeSlot().isBefore(NOON))
                .map(todo -> AdminTodosResponse.toItem(todo, creators))
                .toList();

        return new AdminTodosResponse(date, morningTodos, afternoonTodos);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            throw CustomException.of(TodoErrorCode.INVALID_TODO_DATE);
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw CustomException.of(TodoErrorCode.INVALID_TODO_DATE);
        }
    }

    private LocalDate parseOptionalDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw CustomException.of(TodoErrorCode.INVALID_TODO_INFORMATION);
        }
    }

    private LocalTime parseOptionalTime(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            throw CustomException.of(TodoErrorCode.INVALID_TODO_INFORMATION);
        }
    }

    private String parseOptionalDescription(String value) {
        if (value == null) {
            return null;
        }
        String description = value.trim();
        if (description.isEmpty()) {
            throw CustomException.of(TodoErrorCode.INVALID_TODO_INFORMATION);
        }
        return description;
    }

    private void validateUpdateAccess(Todo todo, Long organizationId) {
        if (!Objects.equals(todo.getOrganizationId(), organizationId)) {
            throw CustomException.of(TodoErrorCode.TODO_UPDATE_ACCESS_DENIED);
        }
    }

    private void validateDeleteAccess(Todo todo, Long organizationId) {
        if (!Objects.equals(todo.getOrganizationId(), organizationId)) {
            throw CustomException.of(TodoErrorCode.TODO_DELETE_ACCESS_DENIED);
        }
    }

    private Map<Long, User> findCreators(List<Todo> todos) {
        List<Long> creatorIds = todos.stream()
                .map(Todo::getCreatedBy)
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
