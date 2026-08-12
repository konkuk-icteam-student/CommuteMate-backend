package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.repository.WorkScheduleSettingRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.ScheduleErrorCode;
import com.better.CommuteMate.schedule.controller.admin.dtos.AdminWorkScheduleDeleteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminWorkScheduleDeletionService {

    private static final List<CodeType> DELETABLE_STATUSES =
            List.of(CodeType.WS01, CodeType.WS02);

    private final WorkSchedulesRepository scheduleRepository;
    private final WorkScheduleSettingRepository settingRepository;
    private final WorkAttendanceRepository attendanceRepository;

    @Transactional
    public AdminWorkScheduleDeleteResponse delete(
            String scheduleId,
            Long organizationId,
            Long adminId
    ) {
        WorkSchedule schedule = scheduleRepository
                .findByScheduleIdAndUser_OrganizationIdAndStatusCodeIn(
                        scheduleId, organizationId, DELETABLE_STATUSES
                )
                .orElseThrow(() -> CustomException.of(
                        ScheduleErrorCode.ADMIN_WORK_SCHEDULE_NOT_FOUND
                ));

        WorkScheduleSetting setting = settingRepository.findForUpdate(
                        String.valueOf(organizationId),
                        schedule.getDate().getYear(),
                        schedule.getDate().getMonthValue()
                )
                .orElseThrow(() -> CustomException.of(
                        ScheduleErrorCode.ADMIN_SCHEDULE_SETTING_NOT_FOUND
                ));

        if (attendanceRepository.existsBySchedule_ScheduleId(scheduleId)) {
            throw CustomException.of(ScheduleErrorCode.ADMIN_WORK_SCHEDULE_HAS_ATTENDANCE);
        }

        schedule.cancel(String.valueOf(adminId));
        scheduleRepository.flush();

        long currentCount = scheduleRepository
                .countBySettingAndDateAndStartTimeAndEndTimeAndStatusCode(
                        setting,
                        schedule.getDate(),
                        schedule.getStartTime(),
                        schedule.getEndTime(),
                        CodeType.WS02
                );

        return new AdminWorkScheduleDeleteResponse(
                schedule.getScheduleId(),
                schedule.getDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                currentCount,
                setting.getMaxConcurrentWorkers()
        );
    }
}
