package com.better.CommuteMate.task.application;

import com.better.CommuteMate.domain.task.entity.Task;
import com.better.CommuteMate.domain.task.repository.TaskRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.task.controller.dtos.CreateAdminTodoRequest;
import com.better.CommuteMate.task.controller.dtos.UpdateAdminTodoRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class AdminTodoServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock UserRepository userRepository;

    private AdminTodoService service;

    @BeforeEach
    void setUp() {
        service = new AdminTodoService(taskRepository, userRepository);
    }

    @Test
    void returnsTodosSeparatedAtNoonWithUiCompletionFields() {
        LocalDate date = LocalDate.of(2026, 4, 15);
        User creator = User.builder().userId(7L).name("홍길동").build();
        Task morning = task(1L, "신문지 가져오기", LocalTime.of(9, 0), true, 7L);
        Task afternoon = task(2L, "회의실 청소", LocalTime.of(14, 0), false, 7L);
        when(taskRepository.findAdminTodos(10L, date)).thenReturn(List.of(morning, afternoon));
        when(userRepository.findAllById(List.of(7L))).thenReturn(List.of(creator));

        var response = service.getTodos(10L, "2026-04-15");

        assertThat(response.date).isEqualTo(date);
        assertThat(response.morningTodos).singleElement().satisfies(todo -> {
            assertThat(todo.todoId()).isEqualTo(1L);
            assertThat(todo.description()).isEqualTo("신문지 가져오기");
            assertThat(todo.status()).isEqualTo("COMPLETED");
            assertThat(todo.createdBy().name()).isEqualTo("홍길동");
            assertThat(todo.completedByName()).isEqualTo("홍길동");
            assertThat(todo.completedTime()).isEqualTo(LocalTime.of(9, 13));
        });
        assertThat(response.afternoonTodos).singleElement()
                .extracting(todo -> todo.status()).isEqualTo("PENDING");
    }

    @Test
    void rejectsInvalidDateFormat() {
        assertThatThrownBy(() -> service.getTodos(10L, "2026/04/15"))
                .isInstanceOf(CustomException.class)
                .hasMessage("날짜 형식이 올바르지 않습니다.");
    }

    @Test
    void createsPendingTodo() {
        Task saved = Task.builder()
                .taskId(3L)
                .title("Newspaper pickup")
                .taskDate(LocalDate.of(2026, 4, 15))
                .taskTime(LocalTime.of(9, 0))
                .isCompleted(false)
                .createdBy(7L)
                .createdAt(LocalDateTime.of(2026, 4, 15, 8, 30))
                .build();
        when(taskRepository.save(any(Task.class))).thenReturn(saved);

        var response = service.createTodo(
                new CreateAdminTodoRequest("2026-04-15", "09:00", "Newspaper pickup"),
                7L
        );

        assertThat(response.todoId).isEqualTo(3L);
        assertThat(response.status).isEqualTo("PENDING");
        assertThat(response.completed).isFalse();
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(7L);
        assertThat(captor.getValue().getTitle()).isEqualTo("Newspaper pickup");
    }

    @Test
    void updatesOnlyProvidedTodoFields() {
        User admin = User.builder().userId(7L).organizationId(10L).build();
        User creator = User.builder().userId(8L).organizationId(10L).build();
        Task task = Task.builder()
                .taskId(3L)
                .title("기존 업무")
                .taskDate(LocalDate.of(2026, 4, 14))
                .taskTime(LocalTime.of(9, 0))
                .isCompleted(false)
                .createdBy(8L)
                .updatedAt(LocalDateTime.of(2026, 4, 15, 10, 40))
                .build();
        when(taskRepository.findById(3L)).thenReturn(Optional.of(task));
        when(userRepository.findById(7L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(8L)).thenReturn(Optional.of(creator));
        when(taskRepository.saveAndFlush(task)).thenReturn(task);

        var response = service.updateTodo(
                3L,
                new UpdateAdminTodoRequest(null, "14:00", " 회의실 청소 "),
                7L
        );

        assertThat(response.todoId).isEqualTo(3L);
        assertThat(response.date).isEqualTo(LocalDate.of(2026, 4, 14));
        assertThat(response.timeSlot).isEqualTo(LocalTime.of(14, 0));
        assertThat(response.description).isEqualTo("회의실 청소");
        assertThat(response.status).isEqualTo("PENDING");
        assertThat(task.getUpdatedBy()).isEqualTo(7L);
        verify(taskRepository).saveAndFlush(task);
    }

    @Test
    void rejectsEmptyUpdateRequest() {
        User admin = User.builder().userId(7L).organizationId(10L).build();
        User creator = User.builder().userId(8L).organizationId(10L).build();
        Task task = Task.builder().taskId(3L).createdBy(8L).build();
        when(taskRepository.findById(3L)).thenReturn(Optional.of(task));
        when(userRepository.findById(7L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(8L)).thenReturn(Optional.of(creator));

        assertThatThrownBy(() -> service.updateTodo(
                3L,
                new UpdateAdminTodoRequest(null, null, null),
                7L
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage("업무사항 입력값이 올바르지 않습니다.");
    }

    @Test
    void rejectsUpdateFromAnotherOrganization() {
        User admin = User.builder().userId(7L).organizationId(10L).build();
        User creator = User.builder().userId(8L).organizationId(20L).build();
        Task task = Task.builder().taskId(3L).createdBy(8L).build();
        when(taskRepository.findById(3L)).thenReturn(Optional.of(task));
        when(userRepository.findById(7L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(8L)).thenReturn(Optional.of(creator));

        assertThatThrownBy(() -> service.updateTodo(
                3L,
                new UpdateAdminTodoRequest(null, "14:00", null),
                7L
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage("업무사항을 수정할 권한이 없습니다.");
    }

    @Test
    void rejectsMissingTodo() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateTodo(
                999L,
                new UpdateAdminTodoRequest(null, "14:00", null),
                7L
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage("업무사항을 찾을 수 없습니다.");
    }

    private Task task(Long id, String title, LocalTime time, boolean completed, Long createdBy) {
        return Task.builder()
                .taskId(id)
                .title(title)
                .taskDate(LocalDate.of(2026, 4, 15))
                .taskTime(time)
                .isCompleted(completed)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.of(2026, 4, 15, 8, 30))
                .completedByName(completed ? "홍길동" : null)
                .completedTime(completed ? LocalTime.of(9, 13) : null)
                .build();
    }
}
