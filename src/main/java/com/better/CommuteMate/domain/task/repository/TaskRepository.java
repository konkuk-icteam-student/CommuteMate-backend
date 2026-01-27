package com.better.CommuteMate.domain.task.repository;

import com.better.CommuteMate.domain.task.entity.Task;
import com.better.CommuteMate.global.code.CodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * 특정 날짜의 모든 업무 조회 (시간순 정렬)
     */
    @Query("SELECT t FROM Task t JOIN FETCH t.assignee WHERE t.taskDate = :taskDate ORDER BY t.taskTime ASC")
    List<Task> findByTaskDateWithAssignee(@Param("taskDate") LocalDate taskDate);

    /**
     * 특정 날짜 + 업무 유형별 조회
     */
    @Query("SELECT t FROM Task t JOIN FETCH t.assignee WHERE t.taskDate = :taskDate AND t.taskType = :taskType ORDER BY t.taskTime ASC")
    List<Task> findByTaskDateAndTaskTypeWithAssignee(
            @Param("taskDate") LocalDate taskDate,
            @Param("taskType") CodeType taskType);

    /**
     * 특정 담당자의 특정 날짜 업무 조회
     */
    @Query("SELECT t FROM Task t WHERE t.assignee.userId = :assigneeId AND t.taskDate = :taskDate ORDER BY t.taskTime ASC")
    List<Task> findByAssigneeIdAndTaskDate(
            @Param("assigneeId") Long assigneeId,
            @Param("taskDate") LocalDate taskDate);

    /**
     * 특정 날짜 범위의 업무 조회
     */
    @Query("SELECT t FROM Task t JOIN FETCH t.assignee WHERE t.taskDate BETWEEN :startDate AND :endDate ORDER BY t.taskDate ASC, t.taskTime ASC")
    List<Task> findByTaskDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 특정 날짜의 미완료 업무 조회
     */
    @Query("SELECT t FROM Task t JOIN FETCH t.assignee WHERE t.taskDate = :taskDate AND t.isCompleted = false ORDER BY t.taskTime ASC")
    List<Task> findIncompleteTasksByDate(@Param("taskDate") LocalDate taskDate);
}
