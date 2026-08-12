package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.ScheduleErrorCode;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleSlotCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ScheduleValidator {

    private final WorkSchedulesRepository workSchedulesRepository;

    /**
     * 어드민 승인 처리 등 DB에서 직접 조회가 필요한 경우에 사용합니다.
     */
    public boolean isScheduleInsertable(
            WorkScheduleSlotCommand slot,
            WorkScheduleSetting setting
    ) {
        List<WorkSchedule> daySchedules = workSchedulesRepository.findAllByDate(slot.date());
        return isScheduleInsertable(slot.start(), slot.end(),
                setting.getMaxConcurrentWorkers(), daySchedules);
    }

    /**
     * 미리 조회한 해당 날짜 전체 근무 목록으로 동시 근무 제한을 검증합니다.
     * changeWorkSchedules에서 배치 처리 시 사용합니다.
     */
    public boolean isScheduleInsertable(
            WorkScheduleSlotCommand slot,
            int maxConcurrentWorkers,
            List<WorkSchedule> preloadedDaySchedules
    ) {
        return isScheduleInsertable(slot.start(), slot.end(),
                maxConcurrentWorkers, preloadedDaySchedules);
    }

    private boolean isScheduleInsertable(
            LocalTime startTime,
            LocalTime endTime,
            int maxConcurrentWorkers,
            List<WorkSchedule> daySchedules
    ) {
        Set<CodeType> activeStatuses = Set.of(CodeType.WS01, CodeType.WS02);
        LocalTime currentCheckPoint = startTime.plusMinutes(15);

        while (currentCheckPoint.isBefore(endTime)) {
            LocalTime finalCheckPoint = currentCheckPoint;

            long overlappingCount = daySchedules.stream()
                    .filter(s -> activeStatuses.contains(s.getStatusCode()))
                    .filter(s -> s.getStartTime().isBefore(finalCheckPoint)
                            && s.getEndTime().isAfter(finalCheckPoint))
                    .count();

            if (overlappingCount >= maxConcurrentWorkers) {
                return false;
            }

            currentCheckPoint = currentCheckPoint.plusMinutes(30);
        }

        return true;
    }

    /**
     * 1회 최소 근무 시간 1시간 검증
     */
    public void validateMinWorkTime(WorkScheduleSlotCommand slot) {
        long minutes = Duration.between(slot.start(), slot.end()).toMinutes();

        if (minutes < 60) {
            throw CustomException.of(ScheduleErrorCode.MIN_WORK_TIME_NOT_MET);
        }
    }

    /**
     * 월 총 근무 시간 초과 여부 검증
     */
    public void validateMonthlyWorkTime(
            long currentMinutes,
            long newMinutes,
            WorkScheduleSetting setting
    ) {
        if (currentMinutes + newMinutes > setting.getMonthlyRequiredMinutes()) {
            throw CustomException.of(ScheduleErrorCode.TOTAL_WORK_TIME_EXCEEDED);
        }
    }

    /**
     * 주 최대 근무 시간 초과 여부 검증
     */
    public void validateWeeklyWorkTime(
            long currentMinutes,
            long newMinutes,
            WorkScheduleSetting setting
    ) {
        Integer weeklyMaxMinutes = setting.getWeeklyMaxMinutes();

        if (weeklyMaxMinutes != null && currentMinutes + newMinutes > weeklyMaxMinutes) {
            throw CustomException.of(ScheduleErrorCode.WEEKLY_WORK_TIME_EXCEEDED);
        }
    }
}