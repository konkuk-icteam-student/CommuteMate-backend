package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.repository.WorkScheduleSettingRepository;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.ScheduleErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkScheduleSettingService {

    private final WorkScheduleSettingRepository workScheduleSettingRepository;

    @Value("${app.schedule.concurrent.max}")
    private int DEFAULT_MAX_CONCURRENT_WORKERS;

    /**
     * 특정 조직의 특정 연/월 근무 일정 설정을 조회합니다.
     */
    public Optional<WorkScheduleSetting> getSetting(
            String organizationId,
            Integer year,
            Integer month
    ) {
        return workScheduleSettingRepository.findByOrganizationIdAndYearAndMonth(
                organizationId,
                year,
                month
        );
    }

    /**
     * 특정 조직의 특정 연/월 근무 일정 설정을 조회합니다.
     * 설정이 없으면 예외를 발생시킵니다.
     */
    public WorkScheduleSetting getRequiredSetting(
            String organizationId,
            Integer year,
            Integer month
    ) {
        return getSetting(organizationId, year, month)
                .orElseThrow(() -> CustomException.of(ScheduleErrorCode.MONTHLY_SCHEDULE_CONFIG_NOT_FOUND));
    }

    /**
     * 저장된 모든 근무 일정 설정 정보를 조회합니다.
     */
    public List<WorkScheduleSetting> getAllSettings() {
        return workScheduleSettingRepository.findAll();
    }

    /**
     * 특정 시간이 신청 기간 내인지 확인합니다.
     */
    public boolean isCurrentlyInApplyTerm(
            String organizationId,
            LocalDateTime targetTime
    ) {
        WorkScheduleSetting setting = getRequiredSetting(
                organizationId,
                targetTime.getYear(),
                targetTime.getMonthValue()
        );

        return setting.isApplyPeriod(LocalDateTime.now());
    }

    /**
     * 근무 신청 기간을 설정합니다.
     */
    @Transactional
    public WorkScheduleSetting setApplyTerm(
            String organizationId,
            Integer year,
            Integer month,
            LocalDateTime applyStartAt,
            LocalDateTime applyEndAt,
            String updatedBy
    ) {
        if (!applyStartAt.isBefore(applyEndAt)) {
            throw CustomException.of(ScheduleErrorCode.INVALID_APPLY_TERM);
        }

        Optional<WorkScheduleSetting> existingSetting =
                workScheduleSettingRepository.findByOrganizationIdAndYearAndMonth(
                        organizationId,
                        year,
                        month
                );

        if (existingSetting.isPresent()) {
            WorkScheduleSetting setting = existingSetting.get();
            setting.updateApplyPeriod(
                    applyStartAt,
                    applyEndAt,
                    updatedBy
            );
            return setting;
        }

        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .organizationId(organizationId)
                .year(year)
                .month(month)
                .applyStartAt(applyStartAt)
                .applyEndAt(applyEndAt)
                .applyEnabled(true)
                .editEnabled(true)
                .autoApproveEnabled(false)
                .maxConcurrentWorkers(DEFAULT_MAX_CONCURRENT_WORKERS)
                .minWorkUnitMinutes(30)
                .monthlyRequiredMinutes(27 * 60)
                .weeklyMaxMinutes(13 * 60)
                .createdBy(updatedBy)
                .updatedBy(updatedBy)
                .build();

        return workScheduleSettingRepository.save(setting);
    }

    /**
     * 특정 조직의 특정 연/월 근무 규칙을 설정합니다.
     */
    @Transactional
    public WorkScheduleSetting setWorkRule(
            String organizationId,
            Integer year,
            Integer month,
            Integer maxConcurrentWorkers,
            Integer minWorkUnitMinutes,
            Integer maxWorkUnitMinutes,
            Integer monthlyRequiredMinutes,
            Integer weeklyMinMinutes,
            Integer weeklyMaxMinutes,
            String updatedBy
    ) {
        WorkScheduleSetting setting = getRequiredSetting(
                organizationId,
                year,
                month
        );

        setting.updateWorkRule(
                maxConcurrentWorkers,
                minWorkUnitMinutes,
                maxWorkUnitMinutes,
                monthlyRequiredMinutes,
                weeklyMinMinutes,
                weeklyMaxMinutes,
                updatedBy
        );

        return setting;
    }

    /**
     * 신청 기간 기본값을 계산합니다.
     * 규칙: 해당 월의 전달 23일 00:00 ~ 전달 27일 00:00
     */
    public LocalDateTime getDefaultApplyStartAt(Integer year, Integer month) {
        YearMonth targetMonth = YearMonth.of(year, month);
        return targetMonth.minusMonths(1)
                .atDay(23)
                .atStartOfDay();
    }

    /**
     * 신청 기간 기본값을 계산합니다.
     * 규칙: 해당 월의 전달 27일 00:00
     */
    public LocalDateTime getDefaultApplyEndAt(Integer year, Integer month) {
        YearMonth targetMonth = YearMonth.of(year, month);
        return targetMonth.minusMonths(1)
                .atDay(27)
                .atStartOfDay();
    }
}