package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.schedule.application.dtos.MonthlyScheduleConfigCommand;
import com.better.CommuteMate.schedule.application.dtos.SetApplyTermCommand;
import com.better.CommuteMate.schedule.application.exceptions.ScheduleErrorCode;
import com.better.CommuteMate.schedule.application.exceptions.ScheduleConfigException;
import com.better.CommuteMate.schedule.application.exceptions.response.ApplyTermValidationResponseDetail;
import com.better.CommuteMate.domain.schedule.entity.MonthlyScheduleConfig;
import com.better.CommuteMate.domain.schedule.repository.MonthlyScheduleConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlyScheduleConfigService {

    private final MonthlyScheduleConfigRepository monthlyScheduleConfigRepository;

    @Value("${app.schedule.concurrent.max}")
    private int DEFAULT_MAX_CONCURRENT_SCHEDULES;

    @Transactional
    public MonthlyScheduleConfig setMonthlyLimit(MonthlyScheduleConfigCommand command) {
        Optional<MonthlyScheduleConfig> existingLimit = monthlyScheduleConfigRepository
                .findByScheduleYearAndScheduleMonth(command.scheduleYear(), command.scheduleMonth());

        if (existingLimit.isPresent()) {
            // 업데이트
            MonthlyScheduleConfig limit = existingLimit.get();
            limit.setMaxConcurrent(command.maxConcurrent());
            limit.setUpdatedBy(command.userId());
            return monthlyScheduleConfigRepository.save(limit);
        } else {
            // 신규 생성 - Command에서 계산된 기본 신청 기간 사용
            MonthlyScheduleConfig newLimit = MonthlyScheduleConfig.builder()
                    .scheduleYear(command.scheduleYear())
                    .scheduleMonth(command.scheduleMonth())
                    .maxConcurrent(command.maxConcurrent())
                    .applyStartTime(command.applyStartTime())
                    .applyEndTime(command.applyEndTime())
                    .createdBy(command.userId())
                    .updatedBy(command.userId())
                    .build();
            return monthlyScheduleConfigRepository.save(newLimit);
        }
    }

    public Optional<MonthlyScheduleConfig> getMonthlyLimit(Integer scheduleYear, Integer scheduleMonth) {
        return monthlyScheduleConfigRepository.findByScheduleYearAndScheduleMonth(scheduleYear, scheduleMonth);
    }

    public List<MonthlyScheduleConfig> getAllMonthlyLimits() {
        return monthlyScheduleConfigRepository.findAll();
    }

    /**
     * startTime 시간이 신청 기간(applyStartTime ~ applyEndTime) 내인지 확인
     */
    public boolean isCurrentlyInApplyTerm(LocalDateTime startTime) {
        LocalDateTime now = LocalDateTime.now();
        int startTimeYear = startTime.getYear();
        int startTimeMonth = startTime.getMonthValue();

        Optional<MonthlyScheduleConfig> config = getMonthlyLimit(startTimeYear, startTimeMonth);

        if (config.isEmpty()) {
            return false;
        }

        MonthlyScheduleConfig monthlyConfig = config.get();
        LocalDateTime applyStartTime = monthlyConfig.getApplyStartTime();
        LocalDateTime applyEndTime = monthlyConfig.getApplyEndTime();
        // 현재 시간이 신청 기간 내인지 확인
        return now.isAfter(applyStartTime) && now.isBefore(applyEndTime);
    }

    @Transactional
    public MonthlyScheduleConfig setApplyTerm(SetApplyTermCommand command) {
        // 신청 기간 유효성 검증 (시작 시간 < 종료 시간)
        if (command.applyStartTime().isAfter(command.applyEndTime()) ||
            command.applyStartTime().isEqual(command.applyEndTime())) {
            throw ScheduleConfigException.of(
                    ScheduleErrorCode.INVALID_APPLY_TERM,
                    ApplyTermValidationResponseDetail.of(
                            command.applyStartTime().toString(),
                            command.applyEndTime().toString()
                    )
            );
        }

        Optional<MonthlyScheduleConfig> existingConfig = monthlyScheduleConfigRepository
                .findByScheduleYearAndScheduleMonth(command.scheduleYear(), command.scheduleMonth());

        if (existingConfig.isPresent()) {
            // 기존 설정 업데이트
            MonthlyScheduleConfig config = existingConfig.get();
            config.setApplyStartTime(command.applyStartTime());
            config.setApplyEndTime(command.applyEndTime());
            config.setUpdatedBy(command.userId());
            return monthlyScheduleConfigRepository.save(config);
        } else {
            // 신규 설정 생성 (maxConcurrent는 기본값 설정)
            MonthlyScheduleConfig newConfig = MonthlyScheduleConfig.builder()
                    .scheduleYear(command.scheduleYear())
                    .scheduleMonth(command.scheduleMonth())
                    .applyStartTime(command.applyStartTime())
                    .applyEndTime(command.applyEndTime())
                    .maxConcurrent(DEFAULT_MAX_CONCURRENT_SCHEDULES) // 기본값
                    .createdBy(command.userId())
                    .updatedBy(command.userId())
                    .build();
            return monthlyScheduleConfigRepository.save(newConfig);
        }
    }

}