package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.schedule.application.dtos.MonthlyScheduleLimitCommand;
import com.better.CommuteMate.domain.schedule.entity.MonthlyScheduleConfig;
import com.better.CommuteMate.domain.schedule.repository.MonthlyScheduleLimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlyScheduleLimitService {

    private final MonthlyScheduleLimitRepository monthlyScheduleLimitRepository;

    @Transactional
    public MonthlyScheduleConfig setMonthlyLimit(MonthlyScheduleLimitCommand command) {
        Optional<MonthlyScheduleConfig> existingLimit = monthlyScheduleLimitRepository
                .findByScheduleYearAndScheduleMonth(command.scheduleYear(), command.scheduleMonth());

        if (existingLimit.isPresent()) {
            // 업데이트
            MonthlyScheduleConfig limit = existingLimit.get();
            limit.setMaxConcurrent(command.maxConcurrent());
            limit.setUpdatedBy(command.userId());
            return monthlyScheduleLimitRepository.save(limit);
        } else {
            // 신규 생성
            MonthlyScheduleConfig newLimit = MonthlyScheduleConfig.builder()
                    .scheduleYear(command.scheduleYear())
                    .scheduleMonth(command.scheduleMonth())
                    .maxConcurrent(command.maxConcurrent())
                    .createdBy(command.userId())
                    .updatedBy(command.userId())
                    .build();
            return monthlyScheduleLimitRepository.save(newLimit);
        }
    }

    public Optional<MonthlyScheduleConfig> getMonthlyLimit(Integer scheduleYear, Integer scheduleMonth) {
        return monthlyScheduleLimitRepository.findByScheduleYearAndScheduleMonth(scheduleYear, scheduleMonth);
    }

    public List<MonthlyScheduleConfig> getAllMonthlyLimits() {
        return monthlyScheduleLimitRepository.findAll();
    }
}