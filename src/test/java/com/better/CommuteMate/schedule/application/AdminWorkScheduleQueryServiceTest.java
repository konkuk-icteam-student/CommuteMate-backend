package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.entity.WorkUnavailableTime;
import com.better.CommuteMate.domain.schedule.repository.WorkScheduleSettingRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkUnavailableTimeRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminWorkScheduleQueryServiceTest {

    @Mock WorkScheduleSettingRepository settingRepository;
    @Mock WorkSchedulesRepository scheduleRepository;
    @Mock WorkUnavailableTimeRepository unavailableTimeRepository;

    AdminWorkScheduleQueryService service;

    @BeforeEach
    void setUp() {
        service = new AdminWorkScheduleQueryService(
                settingRepository, scheduleRepository, unavailableTimeRepository
        );
    }

    @Test
    @DisplayName("관리자 근로시간표 조회 - 30분 슬롯별 근무자와 신청 불가 상태를 반환한다")
    void returnsThirtyMinuteSlotsWithWorkersAndUnavailableStatus() {
        LocalDate date = LocalDate.of(2026, 4, 15);
        WorkScheduleSetting setting = setting();
        User user = User.builder().userId(1L).name("학생A").build();
        WorkSchedule schedule = WorkSchedule.builder()
                .setting(setting)
                .user(user)
                .date(date)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .statusCode(CodeType.WS02)
                .build();
        WorkUnavailableTime unavailable = WorkUnavailableTime.builder()
                .setting(setting)
                .date(date)
                .startTime(LocalTime.of(9, 30))
                .endTime(LocalTime.of(10, 30))
                .build();

        when(settingRepository.findByOrganizationIdAndYearAndMonth(10L, 2026, 4))
                .thenReturn(Optional.of(setting));
        when(scheduleRepository.findAllBySettingAndDateBetweenAndStatusCodeIn(
                setting, date, date, List.of(CodeType.WS01, CodeType.WS02)
        )).thenReturn(List.of(schedule));
        when(unavailableTimeRepository.findBySettingAndDateBetween(setting, date, date))
                .thenReturn(List.of(unavailable));

        var response = service.getSchedules(10L, "2026-04-15", "2026-04-15", null);

        assertThat(response.maxConcurrentWorkers).isEqualTo(4);
        assertThat(response.hasPrev).isFalse();
        assertThat(response.hasNext).isFalse();
        assertThat(response.days).hasSize(1);
        assertThat(response.days.get(0).slots()).hasSize(18);
        assertThat(response.days.get(0).slots().get(0).status()).isEqualTo("AVAILABLE");
        assertThat(response.days.get(0).slots().get(0).currentCount()).isEqualTo(1);
        assertThat(response.days.get(0).slots().get(0).users().get(0).userName())
                .isEqualTo("학생A");
        assertThat(response.days.get(0).slots().get(1).status()).isEqualTo("UNAVAILABLE");
        assertThat(response.days.get(0).slots().get(17).start()).isEqualTo(LocalTime.of(17, 30));
        assertThat(response.days.get(0).slots().get(17).end()).isEqualTo(LocalTime.of(18, 0));
        assertThat(response.days.get(0).slots().get(17).status()).isEqualTo("AVAILABLE");
        assertThat(response.days.get(0).slots().get(17).currentCount()).isZero();
        assertThat(response.days.get(0).slots().get(17).users()).isEmpty();
    }

    @Test
    @DisplayName("관리자 근로시간표 조회 - 조회 기간이 서로 다른 월이면 실패한다")
    void rejectsCrossMonthRange() {
        assertThatThrownBy(() -> service.getSchedules(
                10L, "2026-04-30", "2026-05-01", null
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage("조회 연도 또는 월 값이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("관리자 근로시간표 조회 - 해당 월 설정이 없으면 빈 배열과 이전·다음 설정 여부를 반환한다")
    void returnsEmptyDaysWhenMonthlySettingDoesNotExist() {
        when(settingRepository.findByOrganizationIdAndYearAndMonth(10L, 2026, 4))
                .thenReturn(Optional.empty());
        when(settingRepository.existsByOrganizationIdAndYearAndMonth(10L, 2026, 3))
                .thenReturn(true);
        when(settingRepository.existsByOrganizationIdAndYearAndMonth(10L, 2026, 5))
                .thenReturn(false);

        var response = service.getSchedules(10L, "2026-04-15", "2026-04-15", null);

        assertThat(response.maxConcurrentWorkers).isEqualTo(4);
        assertThat(response.hasPrev).isTrue();
        assertThat(response.hasNext).isFalse();
        assertThat(response.days).isEmpty();
    }

    private WorkScheduleSetting setting() {
        return WorkScheduleSetting.builder()
                .settingId(1L)
                .organizationId(10L)
                .year(2026)
                .month(4)
                .applyStartAt(LocalDateTime.of(2026, 4, 1, 0, 0))
                .applyEndAt(LocalDateTime.of(2026, 4, 10, 23, 59))
                .maxConcurrentWorkers(4)
                .minWorkUnitMinutes(120)
                .monthlyRequiredMinutes(1620)
                .build();
    }
}
