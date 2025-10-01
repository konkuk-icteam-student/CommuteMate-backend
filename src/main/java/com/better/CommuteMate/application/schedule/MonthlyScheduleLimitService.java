package com.better.CommuteMate.application.schedule;

import com.better.CommuteMate.domain.schedule.entity.MonthlyScheduleLimit;
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
    public MonthlyScheduleLimit setMonthlyLimit(Integer scheduleYear, Integer scheduleMonth, Integer maxConcurrent, Integer userId) {
        Optional<MonthlyScheduleLimit> existingLimit = monthlyScheduleLimitRepository.findByScheduleYearAndScheduleMonth(scheduleYear, scheduleMonth);

        if (existingLimit.isPresent()) {
            // 업데이트
            MonthlyScheduleLimit limit = existingLimit.get();
            limit.setMaxConcurrent(maxConcurrent);
            limit.setUpdatedBy(userId);
            return monthlyScheduleLimitRepository.save(limit);
        } else {
            // 신규 생성
            MonthlyScheduleLimit newLimit = MonthlyScheduleLimit.builder()
                    .scheduleYear(scheduleYear)
                    .scheduleMonth(scheduleMonth)
                    .maxConcurrent(maxConcurrent)
                    .createdBy(userId)
                    .updatedBy(userId)
                    .build();
            return monthlyScheduleLimitRepository.save(newLimit);
        }
    }

    public Optional<MonthlyScheduleLimit> getMonthlyLimit(Integer scheduleYear, Integer scheduleMonth) {
        return monthlyScheduleLimitRepository.findByScheduleYearAndScheduleMonth(scheduleYear, scheduleMonth);
    }

    public List<MonthlyScheduleLimit> getAllMonthlyLimits() {
        return monthlyScheduleLimitRepository.findAll();
    }
}