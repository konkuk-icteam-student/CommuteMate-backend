package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.ScheduleErrorCode;
import com.better.CommuteMate.schedule.controller.schedule.dtos.WorkScheduleRangeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserWorkScheduleQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ScheduleService scheduleService;

    private AdminUserWorkScheduleQueryService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserWorkScheduleQueryService(userRepository, scheduleService);
    }

    @Test
    void getSchedule_ReturnsTargetUsersSchedule() {
        Long userId = 1L;
        Long organizationId = 10L;
        LocalDate startDate = LocalDate.of(2026, 5, 18);
        LocalDate endDate = LocalDate.of(2026, 5, 22);
        User user = User.builder()
                .userId(userId)
                .organizationId(organizationId)
                .name("홍길동")
                .build();
        WorkScheduleRangeResponse response = WorkScheduleRangeResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .maxConcurrentWorkers(4)
                .totalLimitHours(27)
                .usedHours(10)
                .days(List.of())
                .build();

        when(userRepository.findByUserIdAndOrganizationId(userId, organizationId))
                .thenReturn(Optional.of(user));
        when(scheduleService.getScheduleRangeView(userId, organizationId, startDate, endDate))
                .thenReturn(response);

        AdminUserWorkScheduleQueryService.Result result = service.getSchedule(
                userId, organizationId, startDate, endDate
        );

        assertThat(result.userName()).isEqualTo("홍길동");
        assertThat(result.response()).isSameAs(response);
        verify(scheduleService).getScheduleRangeView(userId, organizationId, startDate, endDate);
    }

    @Test
    void getSchedule_WhenUserDoesNotBelongToOrganization_ThrowsNotFound() {
        Long userId = 1L;
        Long organizationId = 10L;
        LocalDate startDate = LocalDate.of(2026, 5, 18);
        LocalDate endDate = LocalDate.of(2026, 5, 22);
        when(userRepository.findByUserIdAndOrganizationId(userId, organizationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSchedule(
                userId, organizationId, startDate, endDate
        ))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(ScheduleErrorCode.ADMIN_WORK_SCHEDULE_TARGET_USER_NOT_FOUND));

        verifyNoInteractions(scheduleService);
    }
}
