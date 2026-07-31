package com.better.CommuteMate.domain.workchangerequest.repository;

import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequestItem;
import com.better.CommuteMate.global.code.CodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkChangeRequestItemRepository extends JpaRepository<WorkChangeRequestItem, Long> {

    List<WorkChangeRequestItem> findByRequest_User_UserIdAndRequest_StatusCodeAndChangeTypeCodeAndDateBetween(
            Long userId,
            CodeType requestStatusCode,
            CodeType changeTypeCode,
            LocalDate startDate,
            LocalDate endDate
    );

    List<WorkChangeRequestItem> findAllByRequest_RequestId(Long requestId);

    List<WorkChangeRequestItem> findAllByRequest_RequestIdIn(List<Long> requestIds);
}
