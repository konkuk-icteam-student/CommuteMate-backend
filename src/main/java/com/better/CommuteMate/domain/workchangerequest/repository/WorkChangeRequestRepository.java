package com.better.CommuteMate.domain.workchangerequest.repository;

import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkChangeRequestRepository extends JpaRepository<WorkChangeRequest, String> {
    List<WorkChangeRequest> findByUser_UserId(String userId);
    List<WorkChangeRequest> findBySchedule_ScheduleId(String scheduleId);
}