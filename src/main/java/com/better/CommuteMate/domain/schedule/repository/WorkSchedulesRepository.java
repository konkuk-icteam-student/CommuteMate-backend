package com.better.CommuteMate.domain.schedule.repository;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkSchedulesRepository extends JpaRepository<WorkSchedule, Integer>{
    // 날짜를 기준으로 근무 일정을 조회
    @Query("SELECT w FROM WorkSchedule w WHERE w.startTime >= :startOfDay AND w.startTime < :endOfDay")
    List<WorkSchedule> findByDate(@Param("startOfDay") LocalDateTime startOfDay,
                                   @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * 특정 사용자의 유효한(삭제되지 않은) 근무 일정을 특정 기간 내에서 조회
     * 상태 코드가 신청(WS01) 또는 승인(WS02)인 것만 포함
     */
    @Query("SELECT w FROM WorkSchedule w WHERE w.user.userId = :userId " +
            "AND w.startTime >= :startOfDay AND w.startTime < :endOfDay " +
            "AND w.isDeleted = false " +
            "AND w.statusCode IN (com.better.CommuteMate.global.code.CodeType.WS01, com.better.CommuteMate.global.code.CodeType.WS02)")
    List<WorkSchedule> findValidSchedulesByUserAndDateRange(@Param("userId") Integer userId,
                                                            @Param("startOfDay") LocalDateTime startOfDay,
                                                            @Param("endOfDay") LocalDateTime endOfDay);

    // UserId와 시작 시간을 기준으로 근무 일정을 조회
    Optional<WorkSchedule> findByUserAndStartTime(User user, LocalDateTime startTimes);

    // 상태 코드로 근무 일정 조회
    List<WorkSchedule> findAllByStatusCode(com.better.CommuteMate.global.code.CodeType statusCode);
}
