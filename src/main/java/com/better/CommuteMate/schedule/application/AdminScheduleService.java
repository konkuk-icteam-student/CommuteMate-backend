package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequest;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.schedule.application.exceptions.ScheduleAllFailureException;
import com.better.CommuteMate.schedule.application.exceptions.ScheduleErrorCode;
import com.better.CommuteMate.schedule.controller.admin.dtos.ApplyRequestResponse;
import com.better.CommuteMate.schedule.controller.dtos.NotificationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.better.CommuteMate.schedule.controller.dtos.ScheduleUpdateMessage;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AdminScheduleService {

    private final WorkChangeRequestRepository workChangeRequestRepository;
    private final WorkSchedulesRepository workSchedulesRepository;
    private final SimpMessagingTemplate messagingTemplate;

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

            // WebSocket 알림 전송
            sendNotification(request.getUser().getUserId(), "CHANGE_REQUEST_PROCESSED",
                    "근무 일정 변경 요청이 " + (statusCode == CodeType.CS02 ? "승인" : "거부") + "되었습니다.");
        }
    }

    /**
     * 근무 신청 요청 목록 조회
     * 상태 코드가 WS01(신청)인 근무 일정을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<ApplyRequestResponse> getApplyRequests() {
        return workSchedulesRepository.findAllByStatusCode(CodeType.WS01).stream()
                .map(ApplyRequestResponse::from)
                .toList();
    }

    /**
     * 근무 신청 요청 처리 (승인/거부)
     *
     * @param scheduleIds 근무 일정 ID List
     * @param statusCode  처리 상태 코드 (WS02: 승인, WS03: 거부)
     * @param adminId     처리하는 관리자 ID
     */
    @Transactional
    public void processApplyRequest(List<Integer> scheduleIds, CodeType statusCode, Integer adminId) {
        // WS02 또는 WS03만 허용
        if (statusCode != CodeType.WS02 && statusCode != CodeType.WS03) {
            throw ScheduleAllFailureException.of(ScheduleErrorCode.SCHEDULE_FAILURE, null);
        }

        for (Integer id : scheduleIds) {
            WorkSchedule schedule = workSchedulesRepository.findById(id)
                    .orElseThrow(() -> ScheduleAllFailureException.of(ScheduleErrorCode.SCHEDULE_NOT_FOUND, null));

            // WS01(신청) 상태인지 확인
            if (schedule.getStatusCode() != CodeType.WS01) {
                throw ScheduleAllFailureException.of(ScheduleErrorCode.SCHEDULE_FAILURE, null);
            }

            schedule.updateStatus(statusCode, adminId);

            // 거부(WS03)인 경우, 혹은 승인(WS02)인 경우 알림 전송
            String messageType = statusCode == CodeType.WS02 ? "SCHEDULE_APPROVED" : "SCHEDULE_REJECTED";
            String messageContent = statusCode == CodeType.WS02 ? "근무 신청이 승인되었습니다." : "근무 신청이 거부되었습니다.";
            
            sendNotification(schedule.getUser().getUserId(), messageType, messageContent);
        }
    }

    /**
     * WebSocket을 통해 특정 사용자에게 실시간 알림을 전송합니다.
     *
     * @param userId  알림을 받을 사용자 ID
     * @param type    알림 유형 (예: SCHEDULE_APPROVED)
     * @param message 알림 메시지 내용
     */
    private void sendNotification(Integer userId, String type, String message) {
        NotificationMessage notification = NotificationMessage.builder()
                .type(type)
                .message(message)
                .build();
        // /queue/notifications 사용자별 경로로 메시지 전송
        // 클라이언트는 /user/queue/notifications 를 구독해야 함
        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/queue/notifications",
                notification
        );
    }
}
