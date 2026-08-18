package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.ScheduleErrorCode;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleRangeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserWorkScheduleQueryService {

    private final UserRepository userRepository;
    private final ScheduleService scheduleService;

    public Result getSchedule(
            Long userId,
            Long organizationId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        User user = userRepository.findByUserIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> CustomException.of(
                        ScheduleErrorCode.ADMIN_WORK_SCHEDULE_TARGET_USER_NOT_FOUND
                ));

        WorkScheduleRangeResponse response = scheduleService.getScheduleRangeView(
                user.getUserId(), organizationId, startDate, endDate
        );
        return new Result(user.getName(), response);
    }

    public record Result(String userName, WorkScheduleRangeResponse response) {
    }
}
