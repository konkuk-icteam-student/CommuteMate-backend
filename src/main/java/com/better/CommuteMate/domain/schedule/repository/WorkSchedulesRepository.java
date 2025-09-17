package com.better.CommuteMate.domain.schedule.repository;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkSchedulesRepository extends JpaRepository<WorkSchedule,String>{
    // DB 구조대로 추후에 JPA Repository 작성
}
