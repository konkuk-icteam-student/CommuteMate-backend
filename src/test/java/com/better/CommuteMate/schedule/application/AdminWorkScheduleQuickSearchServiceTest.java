package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminWorkScheduleQuickSearchServiceTest {

    @Mock UserRepository userRepository;
    @Mock WorkSchedulesRepository scheduleRepository;

    AdminWorkScheduleQuickSearchService service;

    @BeforeEach
    void setUp() {
        service = new AdminWorkScheduleQuickSearchService(
                userRepository, scheduleRepository
        );
    }

    @Test
    @DisplayName("빠른 조회 - 연속 슬롯은 병합하고 끊긴 슬롯은 분리한다")
    void mergesContinuousSlotsAndSeparatesGaps() {
        User user = User.builder().userId(2L).name("박영희").build();
        LocalDate startDate = LocalDate.of(2026, 9, 7);
        LocalDate endDate = LocalDate.of(2026, 9, 11);
        LocalDate thursday = LocalDate.of(2026, 9, 10);
        LocalDate friday = LocalDate.of(2026, 9, 11);

        when(userRepository.findByUserIdAndOrganizationId(2L, 10L))
                .thenReturn(Optional.of(user));
        when(scheduleRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
                2L, startDate, endDate, List.of(CodeType.WS02)
        )).thenReturn(List.of(
                schedule(thursday, 10, 0, 10, 30),
                schedule(friday, 16, 30, 18, 30),
                schedule(thursday, 9, 30, 10, 0),
                schedule(thursday, 11, 0, 11, 30)
        ));

        var response = service.search(
                "2", "2026-09-07", "2026-09-11", 10L
        );

        assertThat(response.userId).isEqualTo("2");
        assertThat(response.userName).isEqualTo("박영희");
        assertThat(response.days).hasSize(2);
        assertThat(response.days.get(0).date()).isEqualTo(thursday);
        assertThat(response.days.get(0).dayOfWeek()).isEqualTo("목");
        assertThat(response.days.get(0).slots()).containsExactly(
                new com.better.CommuteMate.schedule.controller.admin.dtos.AdminWorkScheduleQuickSearchResponse.Slot(
                        LocalTime.of(9, 30), LocalTime.of(10, 30)
                ),
                new com.better.CommuteMate.schedule.controller.admin.dtos.AdminWorkScheduleQuickSearchResponse.Slot(
                        LocalTime.of(11, 0), LocalTime.of(11, 30)
                )
        );
        assertThat(response.days.get(1).dayOfWeek()).isEqualTo("금");
        verify(scheduleRepository).findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
                2L, startDate, endDate, List.of(CodeType.WS02)
        );
    }

    @Test
    @DisplayName("빠른 조회 - 배치가 없으면 빈 days 배열을 반환한다")
    void returnsEmptyDaysWhenNoApprovedScheduleExists() {
        User user = User.builder().userId(2L).name("박영희").build();
        LocalDate startDate = LocalDate.of(2026, 9, 7);
        LocalDate endDate = LocalDate.of(2026, 9, 11);
        when(userRepository.findByUserIdAndOrganizationId(2L, 10L))
                .thenReturn(Optional.of(user));
        when(scheduleRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
                2L, startDate, endDate, List.of(CodeType.WS02)
        )).thenReturn(List.of());

        var response = service.search(
                "2", "2026-09-07", "2026-09-11", 10L
        );

        assertThat(response.days).isEmpty();
    }

    @Test
    @DisplayName("빠른 조회 - 사용자가 없거나 다른 조직이면 실패한다")
    void rejectsUserOutsideAdminOrganization() {
        when(userRepository.findByUserIdAndOrganizationId(2L, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.search(
                "2", "2026-09-07", "2026-09-11", 10L
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("빠른 조회 - 시작일이 종료일보다 늦거나 날짜 형식이 잘못되면 실패한다")
    void rejectsInvalidRange() {
        User user = User.builder().userId(2L).name("박영희").build();
        when(userRepository.findByUserIdAndOrganizationId(2L, 10L))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.search(
                "2", "2026-09-12", "2026-09-11", 10L
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage("조회 기간이 올바르지 않습니다.");
        assertThatThrownBy(() -> service.search(
                "2", "2026-09-xx", "2026-09-11", 10L
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage("조회 기간이 올바르지 않습니다.");
    }

    private WorkSchedule schedule(
            LocalDate date,
            int startHour,
            int startMinute,
            int endHour,
            int endMinute
    ) {
        return WorkSchedule.builder()
                .date(date)
                .startTime(LocalTime.of(startHour, startMinute))
                .endTime(LocalTime.of(endHour, endMinute))
                .statusCode(CodeType.WS02)
                .build();
    }
}
