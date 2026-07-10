package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequest;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.ScheduleErrorCode;
import com.better.CommuteMate.schedule.controller.admin.dtos.AdminUserWorkTimeResponse;
import com.better.CommuteMate.schedule.controller.admin.dtos.ApplyRequestResponse;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleHistoryResponse;
import com.better.CommuteMate.user.controller.dto.UserInfoResponse;
import com.better.CommuteMate.user.controller.dto.UserWorkTimeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminScheduleService {

    private final WorkChangeRequestRepository workChangeRequestRepository;
    private final WorkSchedulesRepository workSchedulesRepository;
    private final WorkAttendanceRepository workAttendanceRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final List<CodeType> VALID_STATUS_CODES = List.of(
            CodeType.WS01,
            CodeType.WS02
    );

    /**
     * 변경 요청 처리 (승인/거부)
     */
    @Transactional
    public void processChangeRequest(List<Long> requestIds, CodeType statusCode, Long adminId) {
        for (Long id : requestIds) {
            WorkChangeRequest request = workChangeRequestRepository.findById(id)
                    .orElseThrow(() -> CustomException.of(ScheduleErrorCode.SCHEDULE_FAILURE));

            if (!request.getStatusCode().equals(CodeType.CS01)) {
                throw CustomException.of(ScheduleErrorCode.SCHEDULE_FAILURE);
            }

            request.setStatusCode(statusCode);
            request.setUpdatedBy(adminId);

            if (statusCode.equals(CodeType.CS02)) {
                WorkSchedule schedule = request.getSchedule();
                schedule.approveChangeRequest(String.valueOf(adminId), request.getTypeCode());
            }
        }
    }

    /**
     * 근무 신청 요청 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ApplyRequestResponse> getApplyRequests() {
        return workSchedulesRepository.findAllByStatusCode(CodeType.WS01).stream()
                .map(ApplyRequestResponse::from)
                .toList();
    }

    /**
     * 특정 사용자의 근무 시간 조회
     */
    @Transactional(readOnly = true)
    public UserWorkTimeResponse getUserWorkTime(Long userId, Integer year, Integer month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        long totalMinutes = calculateTotalWorkTime(userId, start, end);

        return new UserWorkTimeResponse(totalMinutes, "MONTHLY");
    }

    /**
     * 전체 사용자의 근무 시간 통계 조회
     */
    @Transactional(readOnly = true)
    public List<AdminUserWorkTimeResponse> getWorkTimeSummary(Integer year, Integer month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

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
    public List<WorkScheduleHistoryResponse> getUserWorkHistory(Long userId, Integer year, Integer month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        return getHistoryList(userId, start, end);
    }

    /**
     * 전체 사용자의 근무 이력 조회
     */
    @Transactional(readOnly = true)
    public List<WorkScheduleHistoryResponse> getAllWorkHistory(Integer year, Integer month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        List<WorkSchedule> schedules = workSchedulesRepository
                .findAllByDateBetweenAndStatusCodeIn(start, end, VALID_STATUS_CODES);

        List<WorkScheduleHistoryResponse> historyList = new ArrayList<>();

        for (WorkSchedule schedule : schedules) {
            historyList.add(convertToHistoryResponse(schedule));
        }

        historyList.sort(Comparator.comparing(WorkScheduleHistoryResponse::getStart));

        return historyList;
    }

    private long calculateTotalWorkTime(Long userId, LocalDate start, LocalDate end) {
        List<WorkSchedule> schedules = workSchedulesRepository
                .findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
                        userId,
                        start,
                        end,
                        VALID_STATUS_CODES
                );

        long totalMinutes = 0;

        for (WorkSchedule schedule : schedules) {
            if (schedule.getStatusCode() != CodeType.WS02) {
                continue;
            }

            List<WorkAttendance> attendances =
                    workAttendanceRepository.findBySchedule_ScheduleId(schedule.getScheduleId());

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

        if (checkIn.isEmpty()) {
            return 0;
        }

        LocalDateTime scheduleStart = toDateTime(schedule, schedule.getStartTime());
        LocalDateTime scheduleEnd = toDateTime(schedule, schedule.getEndTime());

        LocalDateTime start = checkIn.get();
        LocalDateTime end = checkOut.orElse(LocalDateTime.now());

        if (start.isBefore(scheduleStart)) {
            start = scheduleStart;
        }

        if (end.isAfter(scheduleEnd)) {
            end = scheduleEnd;
        }

        if (start.isAfter(end)) {
            return 0;
        }

        return Duration.between(start, end).toMinutes();
    }

    private List<WorkScheduleHistoryResponse> getHistoryList(Long userId, LocalDate start, LocalDate end) {
        List<WorkSchedule> schedules = workSchedulesRepository
                .findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
                        userId,
                        start,
                        end,
                        VALID_STATUS_CODES
                );

        List<WorkScheduleHistoryResponse> historyList = new ArrayList<>();

        for (WorkSchedule schedule : schedules) {
            historyList.add(convertToHistoryResponse(schedule));
        }

        historyList.sort(Comparator.comparing(WorkScheduleHistoryResponse::getStart));

        return historyList;
    }

    private WorkScheduleHistoryResponse convertToHistoryResponse(WorkSchedule schedule) {
        List<WorkAttendance> attendances =
                workAttendanceRepository.findBySchedule_ScheduleId(schedule.getScheduleId());

        Optional<WorkAttendance> checkIn = attendances.stream()
                .filter(a -> a.getCheckTypeCode() == CodeType.CT01)
                .findFirst();

        Optional<WorkAttendance> checkOut = attendances.stream()
                .filter(a -> a.getCheckTypeCode() == CodeType.CT02)
                .findFirst();

        LocalDateTime actualStart = checkIn.map(WorkAttendance::getCheckTime).orElse(null);
        LocalDateTime actualEnd = checkOut.map(WorkAttendance::getCheckTime).orElse(null);

        Long duration = null;
        if (actualStart != null && actualEnd != null) {
            duration = Duration.between(actualStart, actualEnd).toMinutes();
        }

        return WorkScheduleHistoryResponse.builder()
                .id(schedule.getScheduleId())
                .userName(schedule.getUser().getName())
                .start(toDateTime(schedule, schedule.getStartTime()))
                .end(toDateTime(schedule, schedule.getEndTime()))
                .status(schedule.getStatusCode())
                .actualStart(actualStart)
                .actualEnd(actualEnd)
                .workDurationMinutes(duration)
                .build();
    }

    private LocalDateTime toDateTime(WorkSchedule schedule, LocalTime time) {
        return LocalDateTime.of(schedule.getDate(), time);
    }
}