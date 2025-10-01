package com.better.CommuteMate.application.schedule;

import com.better.CommuteMate.application.schedule.dtos.WorkScheduleCommand;
import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ScheduleValidator {

    private final WorkSchedulesRepository workSchedulesRepository;

    @Value("${app.schedule.concurrent.max}")
    private int MAX_CONCURRENT_SCHEDULES;

    // 입력 일정의 분 단위가 00분, 30분 만 존재한다고 가정
    public boolean isScheduleInsertable(WorkScheduleCommand slot) {
        LocalDate date = slot.start().toLocalDate();  // 찾고 싶은 날짜
        LocalDateTime startOfDay = date.atStartOfDay();  // 찾고 싶은 날짜
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();   // 찾고 싶은 날짜 + 1일

        List<WorkSchedule> daySchedules = workSchedulesRepository.findByDate(startOfDay,endOfDay);

        LocalTime startTime = slot.start().toLocalTime();
        LocalTime endTime = slot.end().toLocalTime();

        LocalTime currentCheckPoint = startTime.plusMinutes(15);

        while (currentCheckPoint.isBefore(endTime)) {
            LocalTime finalCheckPoint = currentCheckPoint;

            long overlappingCount = daySchedules.stream()
                .filter(schedule ->
                    schedule.getStartTime().toLocalTime().isBefore(finalCheckPoint) && // 15분 시점이 스케줄 범위에 포함되는지
                    schedule.getEndTime().toLocalTime().isAfter(finalCheckPoint))
                .count();

            if (overlappingCount >= MAX_CONCURRENT_SCHEDULES) {
                return false;
            }

            currentCheckPoint = currentCheckPoint.plusMinutes(30);
        }
        return true;
    }


}