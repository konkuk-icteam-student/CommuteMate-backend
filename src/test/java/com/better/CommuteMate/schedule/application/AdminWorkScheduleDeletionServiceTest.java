package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.repository.WorkScheduleSettingRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
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
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminWorkScheduleDeletionServiceTest {

    @Mock WorkSchedulesRepository scheduleRepository;
    @Mock WorkScheduleSettingRepository settingRepository;
    @Mock WorkAttendanceRepository attendanceRepository;

    AdminWorkScheduleDeletionService service;

    @BeforeEach
    void setUp() {
        service = new AdminWorkScheduleDeletionService(
                scheduleRepository, settingRepository, attendanceRepository
        );
    }

    @Test
    @DisplayName("관리자 근로 시간표 삭제 - WS04로 변경하고 삭제 후 잔여 인원을 반환한다")
    void softDeletesScheduleAndReturnsRemainingCount() {
        WorkScheduleSetting setting = setting();
        WorkSchedule schedule = schedule(setting);

        when(scheduleRepository.findByScheduleIdAndUser_OrganizationIdAndStatusCodeIn(
                "schedule-id", 10L, List.of(CodeType.WS01, CodeType.WS02)
        )).thenReturn(Optional.of(schedule));
        when(settingRepository.findForUpdate("10", 2026, 9))
                .thenReturn(Optional.of(setting));
        when(attendanceRepository.existsBySchedule_ScheduleId("schedule-id"))
                .thenReturn(false);
        when(scheduleRepository.countBySettingAndDateAndStartTimeAndEndTimeAndStatusCode(
                setting,
                LocalDate.of(2026, 9, 8),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                CodeType.WS02
        )).thenReturn(2L);

        var response = service.delete("schedule-id", 10L, 99L);

        assertThat(schedule.getStatusCode()).isEqualTo(CodeType.WS04);
        assertThat(schedule.getUpdatedBy()).isEqualTo("99");
        assertThat(response.getScheduleId()).isEqualTo("schedule-id");
        assertThat(response.getCurrentCount()).isEqualTo(2);
        assertThat(response.getMaxConcurrentWorkers()).isEqualTo(4);
        verify(scheduleRepository).flush();
    }

    @Test
    @DisplayName("관리자 근로 시간표 삭제 - 다른 조직, 취소 또는 없는 스케줄은 찾을 수 없음 처리한다")
    void rejectsUnknownSchedule() {
        when(scheduleRepository.findByScheduleIdAndUser_OrganizationIdAndStatusCodeIn(
                "unknown", 10L, List.of(CodeType.WS01, CodeType.WS02)
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("unknown", 10L, 99L))
                .isInstanceOf(CustomException.class)
                .hasMessage("근로 시간표를 찾을 수 없습니다.");

        verify(attendanceRepository, never()).existsBySchedule_ScheduleId("unknown");
    }

    @Test
    @DisplayName("관리자 근로 시간표 삭제 - 출퇴근 기록이 있으면 삭제하지 않는다")
    void rejectsScheduleWithAttendance() {
        WorkScheduleSetting setting = setting();
        WorkSchedule schedule = schedule(setting);

        when(scheduleRepository.findByScheduleIdAndUser_OrganizationIdAndStatusCodeIn(
                "schedule-id", 10L, List.of(CodeType.WS01, CodeType.WS02)
        )).thenReturn(Optional.of(schedule));
        when(settingRepository.findForUpdate("10", 2026, 9))
                .thenReturn(Optional.of(setting));
        when(attendanceRepository.existsBySchedule_ScheduleId("schedule-id"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.delete("schedule-id", 10L, 99L))
                .isInstanceOf(CustomException.class)
                .hasMessage("출퇴근 기록이 있어 삭제할 수 없습니다.");

        assertThat(schedule.getStatusCode()).isEqualTo(CodeType.WS02);
        verify(scheduleRepository, never()).flush();
    }

    private WorkScheduleSetting setting() {
        return WorkScheduleSetting.builder()
                .organizationId("10")
                .year(2026)
                .month(9)
                .maxConcurrentWorkers(4)
                .build();
    }

    private WorkSchedule schedule(WorkScheduleSetting setting) {
        return WorkSchedule.builder()
                .scheduleId("schedule-id")
                .setting(setting)
                .date(LocalDate.of(2026, 9, 8))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(9, 30))
                .statusCode(CodeType.WS02)
                .build();
    }
}
