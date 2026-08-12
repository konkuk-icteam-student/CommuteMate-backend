package com.better.CommuteMate.domain.todo.repository;

import com.better.CommuteMate.domain.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {

    @Query("""
            select todo
            from Todo todo
            where todo.organizationId = :organizationId
              and todo.date = :date
            order by todo.timeSlot asc, todo.todoId asc
            """)
    List<Todo> findByOrganizationIdAndDate(
            @Param("organizationId") Long organizationId,
            @Param("date") LocalDate date
    );

    long countByOrganizationIdAndDate(Long organizationId, LocalDate date);

    long countByOrganizationIdAndDateAndIsCompleted(Long organizationId, LocalDate date, Boolean isCompleted);
}
