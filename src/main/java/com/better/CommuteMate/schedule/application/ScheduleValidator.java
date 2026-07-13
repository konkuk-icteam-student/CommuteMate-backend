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

@Component
@RequiredArgsConstructor
public class ScheduleValidator {

    private final WorkSchedulesRepository workSchedulesRepository;

    /**
     * 변경사항 API의 슬롯 기준으로 동시 근무 제한을 검증합니다.
     * addSlots 처리 시 사용합니다.
     */
    public boolean isScheduleInsertable(
            WorkScheduleSlotCommand slot,
            WorkScheduleSetting setting
    ) {
        return isScheduleInsertable(
                slot.date(),
                slot.start(),
                slot.end(),
                setting.getMaxConcurrentWorkers()
        );
    }

    /**
     * 해당 날짜, 시작 시간, 종료 시간을 기준으로 동시 근무 제한을 검증합니다.
     */
    private boolean isScheduleInsertable(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            int maxConcurrentWorkers
    ) {
        List<WorkSchedule> daySchedules = workSchedulesRepository.findAllByDate(date);

        LocalTime currentCheckPoint = startTime.plusMinutes(15);

        while (currentCheckPoint.isBefore(endTime)) {
            LocalTime finalCheckPoint = currentCheckPoint;

            long overlappingCount = daySchedules.stream()
                    .filter(schedule -> !schedule.getStatusCode().equals(CodeType.WS04))
                    .filter(schedule ->
                            schedule.getStatusCode().equals(CodeType.WS01)
                                    || schedule.getStatusCode().equals(CodeType.WS02)
                    )
                    .filter(schedule ->
                            schedule.getStartTime().isBefore(finalCheckPoint)
                                    && schedule.getEndTime().isAfter(finalCheckPoint)
                    )
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