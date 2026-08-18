package com.better.CommuteMate.task.application;

import com.better.CommuteMate.domain.todo.entity.Todo;
import com.better.CommuteMate.domain.todo.entity.TodoCompletion;
import com.better.CommuteMate.domain.todo.repository.TodoCompletionRepository;
import com.better.CommuteMate.domain.todo.repository.TodoRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.task.controller.dtos.CreateAdminTodoRequest;
import com.better.CommuteMate.task.controller.dtos.UpdateAdminTodoRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTodoServiceTest {

    @Mock TodoRepository todoRepository;
    @Mock TodoCompletionRepository todoCompletionRepository;
    @Mock UserRepository userRepository;

    private AdminTodoService service;

    @BeforeEach
    void setUp() {
        service = new AdminTodoService(todoRepository, todoCompletionRepository, userRepository);
    }

    @Test
    void returnsSameRecurringTodosForRequestedDateWithDateSpecificCompletion() {
        LocalDate date = LocalDate.of(2026, 4, 15);
        User creator = User.builder().userId(7L).name("홍길동").build();
        Todo morning = todo(1L, "신문지 가져오기", LocalTime.of(9, 0), 10L, 7L);
        Todo afternoon = todo(2L, "회의실 청소", LocalTime.of(14, 0), 10L, 7L);
        TodoCompletion completion = completion(morning, date, "홍길동");

        when(todoRepository.findAllByOrganizationIdOrderByTimeSlotAscTodoIdAsc(10L))
                .thenReturn(List.of(morning, afternoon));
        when(todoCompletionRepository.findAllByTodo_OrganizationIdAndDate(10L, date))
                .thenReturn(List.of(completion));
        when(userRepository.findAllById(List.of(7L))).thenReturn(List.of(creator));

        var response = service.getTodos(10L, "2026-04-15");

        assertThat(response.date).isEqualTo(date);
        assertThat(response.morningTodos).singleElement().satisfies(item -> {
            assertThat(item.todoId()).isEqualTo(1L);
            assertThat(item.status()).isEqualTo("COMPLETED");
            assertThat(item.completedByName()).isEqualTo("홍길동");
        });
        assertThat(response.afternoonTodos).singleElement()
                .extracting(item -> item.status()).isEqualTo("PENDING");
    }

    @Test
    void createsRecurringTodoWithoutRequestDate() {
        Todo saved = todo(3L, "신문지 가져오기", LocalTime.of(9, 0), 10L, 7L);
        when(todoRepository.save(any(Todo.class))).thenReturn(saved);

        var response = service.createTodo(
                new CreateAdminTodoRequest("09:00", "신문지 가져오기"), 7L, 10L
        );

        assertThat(response.todoId).isEqualTo(3L);
        assertThat(response.timeSlot).isEqualTo(LocalTime.of(9, 0));
        ArgumentCaptor<Todo> captor = ArgumentCaptor.forClass(Todo.class);
        verify(todoRepository).save(captor.capture());
        assertThat(captor.getValue().getOrganizationId()).isEqualTo(10L);
        assertThat(captor.getValue().getDescription()).isEqualTo("신문지 가져오기");
    }

    @Test
    void updatesRecurringTodoForEveryDate() {
        Todo existing = todo(3L, "기존 업무", LocalTime.of(9, 0), 10L, 8L);
        when(todoRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(todoRepository.saveAndFlush(existing)).thenReturn(existing);

        var response = service.updateTodo(
                3L, new UpdateAdminTodoRequest("14:00", " 회의실 청소 "), 7L, 10L
        );

        assertThat(response.timeSlot).isEqualTo(LocalTime.of(14, 0));
        assertThat(response.description).isEqualTo("회의실 청소");
        verify(todoRepository).saveAndFlush(existing);
    }

    @Test
    void rejectsEmptyUpdateRequest() {
        Todo existing = todo(3L, "업무", LocalTime.of(9, 0), 10L, 8L);
        when(todoRepository.findById(3L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.updateTodo(
                3L, new UpdateAdminTodoRequest(null, null), 7L, 10L
        )).isInstanceOf(CustomException.class);
    }

    @Test
    void deletesRecurringTodoAndAllCompletionHistory() {
        Todo existing = todo(3L, "업무", LocalTime.of(9, 0), 10L, 8L);
        when(todoRepository.findById(3L)).thenReturn(Optional.of(existing));

        service.deleteTodo(3L, 7L, 10L);

        verify(todoCompletionRepository).deleteAllByTodo_TodoId(3L);
        verify(todoRepository).delete(existing);
    }

    @Test
    void rejectsUpdateAndDeleteFromAnotherOrganization() {
        Todo existing = todo(3L, "업무", LocalTime.of(9, 0), 20L, 8L);
        when(todoRepository.findById(3L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.updateTodo(
                3L, new UpdateAdminTodoRequest("14:00", null), 7L, 10L
        )).isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> service.deleteTodo(3L, 7L, 10L))
                .isInstanceOf(CustomException.class);

        verify(todoRepository, never()).delete(existing);
    }

    @Test
    void completesTodoOnlyForRequestedDate() {
        LocalDate date = LocalDate.of(2026, 4, 15);
        Todo todo = todo(1L, "커피머신 청소", LocalTime.of(9, 0), 10L, 7L);
        when(todoRepository.findById(1L)).thenReturn(Optional.of(todo));
        when(todoCompletionRepository.findByTodo_TodoIdAndDate(1L, date))
                .thenReturn(Optional.empty());
        when(todoCompletionRepository.save(any(TodoCompletion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(todoRepository.findAllByOrganizationIdOrderByTimeSlotAscTodoIdAsc(10L))
                .thenReturn(List.of(todo));
        when(todoCompletionRepository.findAllByTodo_OrganizationIdAndDate(10L, date))
                .thenAnswer(invocation -> List.of(completion(todo, date, "홍길동")));

        var response = service.checkTodo(1L, "2026-04-15", true, 7L, 10L, "홍길동");

        assertThat(response.date).isEqualTo(date);
        assertThat(response.todo.status()).isEqualTo("COMPLETED");
        assertThat(response.summary.completedCount()).isEqualTo(1);
        assertThat(response.summary.totalCount()).isEqualTo(1);
    }

    @Test
    void uncompletesTodoOnlyForRequestedDate() {
        LocalDate date = LocalDate.of(2026, 4, 15);
        Todo todo = todo(1L, "커피머신 청소", LocalTime.of(9, 0), 10L, 7L);
        TodoCompletion completion = completion(todo, date, "홍길동");
        when(todoRepository.findById(1L)).thenReturn(Optional.of(todo));
        when(todoCompletionRepository.findByTodo_TodoIdAndDate(1L, date))
                .thenReturn(Optional.of(completion));
        when(todoRepository.findAllByOrganizationIdOrderByTimeSlotAscTodoIdAsc(10L))
                .thenReturn(List.of(todo));
        when(todoCompletionRepository.findAllByTodo_OrganizationIdAndDate(10L, date))
                .thenReturn(List.of());

        var response = service.checkTodo(1L, "2026-04-15", false, 7L, 10L, "홍길동");

        verify(todoCompletionRepository).delete(completion);
        assertThat(response.todo.status()).isEqualTo("PENDING");
        assertThat(response.summary.completedCount()).isZero();
    }

    @Test
    void completionOnOneDateDoesNotAppearOnAnotherDate() {
        Todo todo = todo(1L, "커피머신 청소", LocalTime.of(9, 0), 10L, 7L);
        when(todoRepository.findAllByOrganizationIdOrderByTimeSlotAscTodoIdAsc(10L))
                .thenReturn(List.of(todo));
        when(todoCompletionRepository.findAllByTodo_OrganizationIdAndDate(
                10L, LocalDate.of(2026, 4, 16)))
                .thenReturn(List.of());
        when(userRepository.findAllById(List.of(7L))).thenReturn(List.of());

        var response = service.getTodos(10L, "2026-04-16");

        assertThat(response.morningTodos).singleElement()
                .extracting(item -> item.status()).isEqualTo("PENDING");
    }

    @Test
    void rejectsInvalidDateFormat() {
        assertThatThrownBy(() -> service.getTodos(10L, "2026/04/15"))
                .isInstanceOf(CustomException.class);
    }

    private Todo todo(Long id, String description, LocalTime time,
            Long organizationId, Long createdBy) {
        return Todo.builder()
                .todoId(id)
                .organizationId(organizationId)
                .description(description)
                .date(LocalDate.of(1970, 1, 1))
                .timeSlot(time)
                .isCompleted(false)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.of(2026, 4, 15, 8, 30))
                .updatedAt(LocalDateTime.of(2026, 4, 15, 8, 30))
                .build();
    }

    private TodoCompletion completion(Todo todo, LocalDate date, String userName) {
        return TodoCompletion.builder()
                .todo(todo)
                .date(date)
                .completedByName(userName)
                .completedTime(LocalTime.of(9, 13))
                .completedBy(7L)
                .build();
    }
}
