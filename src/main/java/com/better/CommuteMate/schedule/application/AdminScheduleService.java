package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequest;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.schedule.application.exceptions.ScheduleAllFailureException;
import com.better.CommuteMate.schedule.application.exceptions.ScheduleErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminScheduleService {

    private final WorkChangeRequestRepository workChangeRequestRepository;

    /**
     * 변경 요청 처리 (승인/거부)
     *
     * @param requestIds  변경 요청 ID List
     * @param statusCode 처리 상태 코드 (CS02: 승인, CS03: 거부)
     * @param adminId    처리하는 관리자 ID
     */
    @Transactional
    public void processChangeRequest(List<Integer> requestIds, CodeType statusCode, Integer adminId) {
        // 변경 요청 조회
        for (Integer id : requestIds) {
            WorkChangeRequest request = workChangeRequestRepository.findById(id)
                    .orElseThrow(() -> ScheduleAllFailureException.of(
                            ScheduleErrorCode.SCHEDULE_FAILURE,
                            null));
            if (!request.getStatusCode().equals(CodeType.CS01)) { // CS01: 대기 상태가 아니라면 오류 반환
                throw ScheduleAllFailureException.of(
                        ScheduleErrorCode.SCHEDULE_FAILURE,
                        null);
            }
            // 요청 상태 업데이트
            request.setStatusCode(statusCode);
            request.setUpdatedBy(adminId);

            // 승인인 경우에만 연결된 WorkSchedule 업데이트
            if (statusCode.equals(CodeType.CS02)) { // CS02: 승인
                WorkSchedule schedule = request.getSchedule();
                schedule.approveChangeRequest(adminId, request.getTypeCode());
            }
        }
        // 거부인 경우(CS03)는 WorkSchedule을 변경하지 않음
    }
}
