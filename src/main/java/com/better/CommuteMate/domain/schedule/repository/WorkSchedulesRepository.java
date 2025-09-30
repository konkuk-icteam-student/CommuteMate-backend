package com.better.CommuteMate.domain.schedule.repository;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkSchedulesRepository extends JpaRepository<WorkSchedule, Integer>{

    @Query("SELECT w FROM WorkSchedule w WHERE DATE(w.startTime) = :date")
    List<WorkSchedule> findByDate(@Param("date") LocalDate date);
}
