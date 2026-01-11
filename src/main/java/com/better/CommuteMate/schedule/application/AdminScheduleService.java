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
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.schedule.controller.admin.dtos.AdminUserWorkTimeResponse;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleHistoryResponse;
import com.better.CommuteMate.user.controller.dto.UserInfoResponse;
import com.better.CommuteMate.user.controller.dto.UserWorkTimeResponse;
import com.better.CommuteMate.global.exceptions.UserNotFoundException;
import com.better.CommuteMate.global.exceptions.error.GlobalErrorCode;
import com.better.CommuteMate.global.exceptions.response.UserNotFoundResponseDetail;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminScheduleService {

    private final WorkChangeRequestRepository workChangeRequestRepository;
    private final WorkSchedulesRepository workSchedulesRepository;
    private final WorkAttendanceRepository workAttendanceRepository;
    private final UserRepository userRepository;
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
     * 특정 사용자의 근무 시간 조회
     */
    @Transactional(readOnly = true)
    public UserWorkTimeResponse getUserWorkTime(Integer userId, Integer year, Integer month) {
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = start.plusMonths(1);

        long totalMinutes = calculateTotalWorkTime(userId, start, end);
        return new UserWorkTimeResponse(totalMinutes, "MONTHLY");
    }

    /**
     * 전체 사용자의 근무 시간 통계 조회
     */
    @Transactional(readOnly = true)
    public List<AdminUserWorkTimeResponse> getWorkTimeSummary(Integer year, Integer month) {
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = start.plusMonths(1);

        List<User> users = userRepository.findAll();
        List<AdminUserWorkTimeResponse> summaryList = new ArrayList<>();

        for (User user : users) {
            long totalMinutes = calculateTotalWorkTime(user.getUserId(), start, end);
            summaryList.add(AdminUserWorkTimeResponse.builder()
                    .userInfo(new UserInfoResponse(user))
                    .totalMinutes(totalMinutes)
                    .build());
        }
        return summaryList;
    }

    /**
     * 특정 사용자의 근무 이력 조회
     */
    @Transactional(readOnly = true)
    public List<WorkScheduleHistoryResponse> getUserWorkHistory(Integer userId, Integer year, Integer month) {
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = start.plusMonths(1);

        return getHistoryList(userId, start, end);
    }

    /**
     * 전체 사용자의 근무 이력 조회 (사용자별로 그룹화하지 않고 평탄화)
     */
    @Transactional(readOnly = true)
    public List<WorkScheduleHistoryResponse> getAllWorkHistory(Integer year, Integer month) {
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = start.plusMonths(1);

        List<WorkSchedule> schedules = workSchedulesRepository.findByDate(start, end);
        
        List<WorkScheduleHistoryResponse> historyList = new ArrayList<>();
        for (WorkSchedule schedule : schedules) {
            historyList.add(convertToHistoryResponse(schedule));
        }
        historyList.sort(Comparator.comparing(WorkScheduleHistoryResponse::getStart));
        return historyList;
    }

    private long calculateTotalWorkTime(Integer userId, LocalDateTime start, LocalDateTime end) {
        List<WorkSchedule> schedules = workSchedulesRepository.findAllSchedulesByUserAndDateRange(
                userId, start, end);
        
        long totalMinutes = 0;
        for (WorkSchedule schedule : schedules) {
            if (schedule.getStatusCode() != CodeType.WS02) {
                continue;
            }
            List<WorkAttendance> attendances = workAttendanceRepository.findBySchedule_ScheduleId(schedule.getScheduleId());
            totalMinutes += calculateDuration(schedule, attendances);
        }
        return totalMinutes;
    }

    private long calculateDuration(WorkSchedule schedule, List<WorkAttendance> attendances) {
        Optional<LocalDateTime> checkIn = attendances.stream()
                .filter(a -> a.getCheckTypeCode() == CodeType.CT01)
                .map(WorkAttendance::getCheckTime)
                .findFirst();

        Optional<LocalDateTime> checkOut = attendances.stream()
                .filter(a -> a.getCheckTypeCode() == CodeType.CT02)
                .map(WorkAttendance::getCheckTime)
                .findFirst();

        if (checkIn.isEmpty()) return 0;

        LocalDateTime start = checkIn.get();
        LocalDateTime end = checkOut.orElse(LocalDateTime.now());

        if (start.isBefore(schedule.getStartTime())) start = schedule.getStartTime();
        if (end.isAfter(schedule.getEndTime())) end = schedule.getEndTime();

        if (start.isAfter(end)) return 0;

        return Duration.between(start, end).toMinutes();
    }

    private List<WorkScheduleHistoryResponse> getHistoryList(Integer userId, LocalDateTime start, LocalDateTime end) {
        List<WorkSchedule> schedules = workSchedulesRepository.findAllSchedulesByUserAndDateRange(userId, start, end);
        List<WorkScheduleHistoryResponse> historyList = new ArrayList<>();

        for (WorkSchedule schedule : schedules) {
            historyList.add(convertToHistoryResponse(schedule));
        }
        
        historyList.sort(Comparator.comparing(WorkScheduleHistoryResponse::getStart));
        return historyList;
    }

    private WorkScheduleHistoryResponse convertToHistoryResponse(WorkSchedule schedule) {
        List<WorkAttendance> attendances = workAttendanceRepository.findBySchedule_ScheduleId(schedule.getScheduleId());
        
        Optional<WorkAttendance> checkIn = attendances.stream()
                .filter(a -> a.getCheckTypeCode() == CodeType.CT01).findFirst();
        Optional<WorkAttendance> checkOut = attendances.stream()
                .filter(a -> a.getCheckTypeCode() == CodeType.CT02).findFirst();

        LocalDateTime actualStart = checkIn.map(WorkAttendance::getCheckTime).orElse(null);
        LocalDateTime actualEnd = checkOut.map(WorkAttendance::getCheckTime).orElse(null);
        Long duration = null;
        if (actualStart != null && actualEnd != null) {
            duration = Duration.between(actualStart, actualEnd).toMinutes();
        }

        return WorkScheduleHistoryResponse.builder()
                .id(schedule.getScheduleId())
                .start(schedule.getStartTime())
                .end(schedule.getEndTime())
                .status(schedule.getStatusCode())
                .actualStart(actualStart)
                .actualEnd(actualEnd)
                .workDurationMinutes(duration)
                .build();
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
