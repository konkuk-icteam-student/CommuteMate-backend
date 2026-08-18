package com.better.CommuteMate.domain.todo.repository;

import com.better.CommuteMate.domain.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {

    List<Todo> findAllByOrganizationIdOrderByTimeSlotAscTodoIdAsc(Long organizationId);
}
