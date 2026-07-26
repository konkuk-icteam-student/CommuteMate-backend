package com.better.CommuteMate.domain.workchangerequest.repository;

import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequestItem;
import com.better.CommuteMate.global.code.CodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkChangeRequestItemRepository extends JpaRepository<WorkChangeRequestItem, Long> {

    /**
     * 특정 사용자의 PENDING 상태 변경 요청 항목 조회 (날짜 범위 필터 포함)
     * user는 work_change_request.user_id를 통해 조회
     */
    List<WorkChangeRequestItem> findByRequest_User_UserIdAndRequest_StatusCodeAndChangeTypeCodeAndDateBetween(
            Long userId,
            CodeType requestStatusCode,
            CodeType changeTypeCode,
            LocalDate startDate,
            LocalDate endDate
    );

    List<WorkChangeRequestItem> findAllByRequest_RequestId(Long requestId);
}
