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
    // UserId와 시작 시간을 기준으로 근무 일정을 조회
    Optional<WorkSchedule> findByUserAndStartTime(User user, LocalDateTime startTimes);
}
