package com.better.CommuteMate.task.application;

import com.better.CommuteMate.domain.todo.entity.Todo;
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
    @Mock UserRepository userRepository;

    private AdminTodoService service;

    @BeforeEach
    void setUp() {
        service = new AdminTodoService(todoRepository, userRepository);
    }

    @Test
    void returnsTodosSeparatedAtNoonWithUiCompletionFields() {
        LocalDate date = LocalDate.of(2026, 4, 15);
        User creator = User.builder().userId(7L).name("홍길동").build();
        Todo morning = todo(1L, "신문지 가져오기", LocalTime.of(9, 0), true, 10L, 7L);
        Todo afternoon = todo(2L, "회의실 청소", LocalTime.of(14, 0), false, 10L, 7L);
        when(todoRepository.findByOrganizationIdAndDate(10L, date)).thenReturn(List.of(morning, afternoon));
        when(userRepository.findAllById(List.of(7L))).thenReturn(List.of(creator));

        var response = service.getTodos(10L, "2026-04-15");

        assertThat(response.date).isEqualTo(date);
        assertThat(response.morningTodos).singleElement().satisfies(item -> {
            assertThat(item.todoId()).isEqualTo(1L);
            assertThat(item.description()).isEqualTo("신문지 가져오기");
            assertThat(item.status()).isEqualTo("COMPLETED");
            assertThat(item.createdBy().name()).isEqualTo("홍길동");
            assertThat(item.completedByName()).isEqualTo("홍길동");
            assertThat(item.completedTime()).isEqualTo(LocalTime.of(9, 13));
        });
        assertThat(response.afternoonTodos).singleElement()
                .extracting(item -> item.status()).isEqualTo("PENDING");
    }

    @Test
    void rejectsInvalidDateFormat() {
        assertThatThrownBy(() -> service.getTodos(10L, "2026/04/15"))
                .isInstanceOf(CustomException.class)
                .hasMessage("날짜 형식이 올바르지 않습니다.");
    }

    @Test
    void createsPendingTodo() {
        Todo saved = Todo.builder()
                .todoId(3L)
                .organizationId(10L)
                .description("Newspaper pickup")
                .date(LocalDate.of(2026, 4, 15))
                .timeSlot(LocalTime.of(9, 0))
                .isCompleted(false)
                .createdBy(7L)
                .createdAt(LocalDateTime.of(2026, 4, 15, 8, 30))
                .build();
        when(todoRepository.save(any(Todo.class))).thenReturn(saved);

        var response = service.createTodo(
                new CreateAdminTodoRequest("2026-04-15", "09:00", "Newspaper pickup"),
                7L,
                10L
        );

        assertThat(response.todoId).isEqualTo(3L);
        assertThat(response.status).isEqualTo("PENDING");
        assertThat(response.completed).isFalse();
        ArgumentCaptor<Todo> captor = ArgumentCaptor.forClass(Todo.class);
        verify(todoRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(7L);
        assertThat(captor.getValue().getOrganizationId()).isEqualTo(10L);
        assertThat(captor.getValue().getDescription()).isEqualTo("Newspaper pickup");
    }

    @Test
    void updatesOnlyProvidedTodoFields() {
        Todo existingTodo = Todo.builder()
                .todoId(3L)
                .organizationId(10L)
                .description("기존 업무")
                .date(LocalDate.of(2026, 4, 14))
                .timeSlot(LocalTime.of(9, 0))
                .isCompleted(false)
                .createdBy(8L)
                .updatedAt(LocalDateTime.of(2026, 4, 15, 10, 40))
                .build();
        when(todoRepository.findById(3L)).thenReturn(Optional.of(existingTodo));
        when(todoRepository.saveAndFlush(existingTodo)).thenReturn(existingTodo);

        var response = service.updateTodo(
                3L,
                new UpdateAdminTodoRequest(null, "14:00", " 회의실 청소 "),
                7L,
                10L
        );

        assertThat(response.todoId).isEqualTo(3L);
        assertThat(response.date).isEqualTo(LocalDate.of(2026, 4, 14));
        assertThat(response.timeSlot).isEqualTo(LocalTime.of(14, 0));
        assertThat(response.description).isEqualTo("회의실 청소");
        assertThat(response.status).isEqualTo("PENDING");
        verify(todoRepository).saveAndFlush(existingTodo);
    }

    @Test
    void rejectsEmptyUpdateRequest() {
        Todo existingTodo = Todo.builder().todoId(3L).organizationId(10L).createdBy(8L).build();
        when(todoRepository.findById(3L)).thenReturn(Optional.of(existingTodo));

        assertThatThrownBy(() -> service.updateTodo(
                3L,
                new UpdateAdminTodoRequest(null, null, null),
                7L,
                10L
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage("업무사항 입력값이 올바르지 않습니다.");
    }

    @Test
    void rejectsUpdateFromAnotherOrganization() {
        Todo existingTodo = Todo.builder().todoId(3L).organizationId(20L).createdBy(8L).build();
        when(todoRepository.findById(3L)).thenReturn(Optional.of(existingTodo));

        assertThatThrownBy(() -> service.updateTodo(
                3L,
                new UpdateAdminTodoRequest(null, "14:00", null),
                7L,
                10L
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage("업무사항을 수정할 권한이 없습니다.");
    }

    @Test
    void rejectsMissingTodoOnUpdate() {
        when(todoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateTodo(
                999L,
                new UpdateAdminTodoRequest(null, "14:00", null),
                7L,
                10L
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage("업무사항을 찾을 수 없습니다.");
    }

    @Test
    void deletesTodoInSameOrganization() {
        Todo existingTodo = Todo.builder().todoId(3L).organizationId(10L).createdBy(8L).build();
        when(todoRepository.findById(3L)).thenReturn(Optional.of(existingTodo));

        service.deleteTodo(3L, 7L, 10L);

        verify(todoRepository).delete(existingTodo);
    }

    @Test
    void rejectsDeleteFromAnotherOrganization() {
        Todo existingTodo = Todo.builder().todoId(3L).organizationId(20L).createdBy(8L).build();
        when(todoRepository.findById(3L)).thenReturn(Optional.of(existingTodo));

        assertThatThrownBy(() -> service.deleteTodo(3L, 7L, 10L))
                .isInstanceOf(CustomException.class)
                .hasMessage("업무사항을 삭제할 권한이 없습니다.");
        verify(todoRepository, never()).delete(existingTodo);
    }

    @Test
    void rejectsMissingTodoOnDelete() {
        when(todoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteTodo(999L, 7L, 10L))
                .isInstanceOf(CustomException.class)
                .hasMessage("업무사항을 찾을 수 없습니다.");
    }

    private Todo todo(Long id, String description, LocalTime time, boolean completed,
            Long organizationId, Long createdBy) {
        return Todo.builder()
                .todoId(id)
                .organizationId(organizationId)
                .description(description)
                .date(LocalDate.of(2026, 4, 15))
                .timeSlot(time)
                .isCompleted(completed)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.of(2026, 4, 15, 8, 30))
                .completedByName(completed ? "홍길동" : null)
                .completedTime(completed ? LocalTime.of(9, 13) : null)
                .build();
    }
}
