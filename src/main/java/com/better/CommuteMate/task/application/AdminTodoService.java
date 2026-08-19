package com.better.CommuteMate.task.application;

import com.better.CommuteMate.domain.todo.entity.Todo;
import com.better.CommuteMate.domain.todo.entity.TodoCompletion;
import com.better.CommuteMate.domain.todo.repository.TodoCompletionRepository;
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
import com.better.CommuteMate.task.controller.dtos.UpdateTodoCompletionResponse;
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
    private final TodoCompletionRepository todoCompletionRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreateAdminTodoResponse createTodo(
            CreateAdminTodoRequest request,
            Long adminId,
            Long organizationId
    ) {
        LocalTime timeSlot;
        try {
            timeSlot = LocalTime.parse(request.timeSlot());
        } catch (DateTimeParseException e) {
            throw CustomException.of(TodoErrorCode.INVALID_TODO_INFORMATION);
        }

        Todo todo = Todo.create(
                organizationId,
                request.description().trim(),
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

        if (request.timeSlot() == null
                && request.description() == null) {
            throw CustomException.of(TodoErrorCode.INVALID_TODO_INFORMATION);
        }

        LocalTime timeSlot = parseOptionalTime(request.timeSlot());
        String description = parseOptionalDescription(request.description());

        todo.update(timeSlot, description, adminId);
        return UpdateAdminTodoResponse.from(todoRepository.saveAndFlush(todo));
    }

    @Transactional
    public void deleteTodo(Long todoId, Long adminId, Long organizationId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> CustomException.of(TodoErrorCode.TODO_NOT_FOUND));
        validateDeleteAccess(todo, organizationId);
        todoCompletionRepository.deleteAllByTodo_TodoId(todoId);
        todoRepository.delete(todo);
    }

    @Transactional
    public UpdateTodoCompletionResponse checkTodo(
            Long todoId,
            String dateValue,
            Boolean isCompleted,
            Long userId,
            Long organizationId,
            String userName
    ) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> CustomException.of(TodoErrorCode.TODO_NOT_FOUND));

        if (!Objects.equals(todo.getOrganizationId(), organizationId)) {
            throw CustomException.of(TodoErrorCode.TODO_CHECK_ACCESS_DENIED);
        }

        LocalDate date = parseDate(dateValue);
        TodoCompletion completion = todoCompletionRepository
                .findByTodo_TodoIdAndDate(todoId, date)
                .orElse(null);

        if (Boolean.TRUE.equals(isCompleted)) {
            if (completion == null) {
                completion = TodoCompletion.builder()
                        .todo(todo)
                        .date(date)
                        .completedByName(userName)
                        .completedTime(LocalTime.now())
                        .completedBy(userId)
                        .build();
            } else {
                completion.update(userName, LocalTime.now(), userId);
            }
            completion = todoCompletionRepository.save(completion);
        } else {
            if (completion != null) {
                todoCompletionRepository.delete(completion);
                completion = null;
            }
        }

        int totalCount = todoRepository.findAllByOrganizationIdOrderByTimeSlotAscTodoIdAsc(organizationId).size();
        int completedCount = todoCompletionRepository
                .findAllByTodo_OrganizationIdAndDate(organizationId, date).size();

        return UpdateTodoCompletionResponse.of(todo, date, completion, completedCount, totalCount);
    }

    public AdminTodosResponse getTodos(Long organizationId, String dateValue) {
        LocalDate date = parseDate(dateValue);
        List<Todo> todos = todoRepository.findAllByOrganizationIdOrderByTimeSlotAscTodoIdAsc(organizationId);
        Map<Long, TodoCompletion> completions = todoCompletionRepository
                .findAllByTodo_OrganizationIdAndDate(organizationId, date)
                .stream()
                .collect(Collectors.toMap(completion -> completion.getTodo().getTodoId(), Function.identity()));
        Map<Long, User> creators = findCreators(todos);

        List<AdminTodosResponse.TodoItem> morningTodos = todos.stream()
                .filter(todo -> todo.getTimeSlot().isBefore(NOON))
                .map(todo -> AdminTodosResponse.toItem(todo, completions.get(todo.getTodoId()), creators))
                .toList();
        List<AdminTodosResponse.TodoItem> afternoonTodos = todos.stream()
                .filter(todo -> !todo.getTimeSlot().isBefore(NOON))
                .map(todo -> AdminTodosResponse.toItem(todo, completions.get(todo.getTodoId()), creators))
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
