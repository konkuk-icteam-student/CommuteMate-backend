package com.better.CommuteMate.task.application;

import com.better.CommuteMate.domain.task.entity.Task;
import com.better.CommuteMate.domain.task.repository.TaskRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.task.controller.dtos.CreateAdminTodoRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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
