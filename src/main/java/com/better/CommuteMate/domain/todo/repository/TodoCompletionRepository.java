package com.better.CommuteMate.domain.todo.repository;

import com.better.CommuteMate.domain.todo.entity.TodoCompletion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TodoCompletionRepository extends JpaRepository<TodoCompletion, Long> {

    List<TodoCompletion> findAllByTodo_OrganizationIdAndDate(Long organizationId, LocalDate date);

    Optional<TodoCompletion> findByTodo_TodoIdAndDate(Long todoId, LocalDate date);

    void deleteAllByTodo_TodoId(Long todoId);
}
