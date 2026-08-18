package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.entity.WorkUnavailableTime;
import com.better.CommuteMate.domain.schedule.repository.WorkScheduleSettingRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkUnavailableTimeRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
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
    @Mock WorkAttendanceRepository attendanceRepository;

    AdminWorkScheduleQueryService service;

    @BeforeEach
    void setUp() {
        service = new AdminWorkScheduleQueryService(
                settingRepository, scheduleRepository, unavailableTimeRepository, attendanceRepository
        );
    }

    @Test
    @DisplayName("관리자 근로시간표 조회 - 30분 슬롯별 근무자와 신청 불가 상태를 반환한다")
    void returnsThirtyMinuteSlotsWithWorkersAndUnavailableStatus() {
        LocalDate date = LocalDate.of(2026, 4, 15);
        WorkScheduleSetting setting = setting();
        User user = User.builder().userId(1L).name("학생A").build();
        WorkSchedule schedule = WorkSchedule.builder()
                .scheduleId(10L)
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
        assertThat(response.days.get(0).slots().get(0).users().get(0).userId())
                .isEqualTo("1");
        assertThat(response.days.get(0).slots().get(0).users().get(0).scheduleId())
                .isEqualTo(10L);
        assertThat(response.days.get(0).slots().get(1).status()).isEqualTo("UNAVAILABLE");
        assertThat(response.days.get(0).slots().get(17).start()).isEqualTo(LocalTime.of(17, 30));
        assertThat(response.days.get(0).slots().get(17).end()).isEqualTo(LocalTime.of(18, 0));
        assertThat(response.days.get(0).slots().get(17).status()).isEqualTo("AVAILABLE");
        assertThat(response.days.get(0).slots().get(17).currentCount()).isZero();
        assertThat(response.days.get(0).slots().get(17).users()).isEmpty();
    }

    @Test
    @DisplayName("관리자 근로시간표 조회 - 09:00~11:00 근무 1건이 4개 슬롯 모두에 동일한 scheduleId로 노출된다")
    void sameScheduleIdAppearsInAllSlotsOfMultiSlotWorkSchedule() {
        LocalDate date = LocalDate.of(2026, 4, 15);
        WorkScheduleSetting setting = setting();
        User user = User.builder().userId(1L).name("학생A").build();
        WorkSchedule schedule = WorkSchedule.builder()
                .scheduleId(42L)
                .setting(setting)
                .user(user)
                .date(date)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .statusCode(CodeType.WS02)
                .build();

        when(settingRepository.findByOrganizationIdAndYearAndMonth(10L, 2026, 4))
                .thenReturn(Optional.of(setting));
        when(scheduleRepository.findAllBySettingAndDateBetweenAndStatusCodeIn(
                setting, date, date, List.of(CodeType.WS01, CodeType.WS02)
        )).thenReturn(List.of(schedule));
        when(unavailableTimeRepository.findBySettingAndDateBetween(setting, date, date))
                .thenReturn(List.of());

        var response = service.getSchedules(10L, "2026-04-15", "2026-04-15", null);

        List<com.better.CommuteMate.schedule.controller.admin.dtos.AdminScheduleRangeResponse.Slot> slots =
                response.days.get(0).slots();

        // 09:00~11:00 → 09:00-09:30, 09:30-10:00, 10:00-10:30, 10:30-11:00 의 4개 슬롯
        var coveredSlots = slots.stream()
                .filter(s -> !s.start().isBefore(LocalTime.of(9, 0))
                        && s.start().isBefore(LocalTime.of(11, 0)))
                .toList();
        assertThat(coveredSlots).hasSize(4);
        coveredSlots.forEach(slot -> {
            assertThat(slot.users()).hasSize(1);
            assertThat(slot.users().get(0).scheduleId()).isEqualTo(42L);
        });

        // 11:00 이후 슬롯에는 해당 사용자가 없어야 한다
        var uncoveredSlots = slots.stream()
                .filter(s -> !s.start().isBefore(LocalTime.of(11, 0)))
                .toList();
        uncoveredSlots.forEach(slot -> assertThat(slot.users()).isEmpty());
    }

    @Test
    @DisplayName("관리자 근로시간표 조회 - 같은 슬롯에 여러 사용자가 있을 때 각자 자기 scheduleId를 갖는다")
    void eachWorkerInSameSlotHasOwnScheduleId() {
        LocalDate date = LocalDate.of(2026, 4, 15);
        WorkScheduleSetting setting = setting();
        User userA = User.builder().userId(1L).name("학생A").build();
        User userB = User.builder().userId(2L).name("학생B").build();
        WorkSchedule scheduleA = WorkSchedule.builder()
                .scheduleId(101L)
                .setting(setting)
                .user(userA)
                .date(date)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(9, 30))
                .statusCode(CodeType.WS02)
                .build();
        WorkSchedule scheduleB = WorkSchedule.builder()
                .scheduleId(202L)
                .setting(setting)
                .user(userB)
                .date(date)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(9, 30))
                .statusCode(CodeType.WS01)
                .build();

        when(settingRepository.findByOrganizationIdAndYearAndMonth(10L, 2026, 4))
                .thenReturn(Optional.of(setting));
        when(scheduleRepository.findAllBySettingAndDateBetweenAndStatusCodeIn(
                setting, date, date, List.of(CodeType.WS01, CodeType.WS02)
        )).thenReturn(List.of(scheduleA, scheduleB));
        when(unavailableTimeRepository.findBySettingAndDateBetween(setting, date, date))
                .thenReturn(List.of());

        var response = service.getSchedules(10L, "2026-04-15", "2026-04-15", null);

        var slot0900 = response.days.get(0).slots().stream()
                .filter(s -> s.start().equals(LocalTime.of(9, 0)))
                .findFirst().orElseThrow();

        assertThat(slot0900.users()).hasSize(2);
        var workerA = slot0900.users().stream()
                .filter(w -> w.userId().equals("1")).findFirst().orElseThrow();
        var workerB = slot0900.users().stream()
                .filter(w -> w.userId().equals("2")).findFirst().orElseThrow();
        assertThat(workerA.scheduleId()).isEqualTo(101L);
        assertThat(workerB.scheduleId()).isEqualTo(202L);
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

    @Test
    @DisplayName("관리자 근로시간표 조회 - 출퇴근 기록에 따라 근무 중, 완료, 미출근 상태를 반환한다")
    void returnsWorkStatusFromAttendanceRecords() {
        LocalDate date = LocalDate.now().minusDays(1);
        WorkScheduleSetting setting = setting();
        WorkSchedule checkedIn = schedule(101L, 1L, "근무중", date, setting);
        WorkSchedule checkedOut = schedule(102L, 2L, "완료", date, setting);
        WorkSchedule absent = schedule(103L, 3L, "미출근", date, setting);
        WorkAttendance checkIn = attendance(checkedIn, CodeType.CT01);
        WorkAttendance checkOut = attendance(checkedOut, CodeType.CT02);

        when(settingRepository.findByOrganizationIdAndYearAndMonth(
                10L, date.getYear(), date.getMonthValue()))
                .thenReturn(Optional.of(setting));
        when(scheduleRepository.findAllBySettingAndDateBetweenAndStatusCodeIn(
                setting, date, date, List.of(CodeType.WS01, CodeType.WS02)))
                .thenReturn(List.of(checkedIn, checkedOut, absent));
        when(unavailableTimeRepository.findBySettingAndDateBetween(setting, date, date))
                .thenReturn(List.of());
        when(attendanceRepository.findAllByScheduleIn(List.of(checkedIn, checkedOut, absent)))
                .thenReturn(List.of(checkIn, checkOut));

        var response = service.getSchedules(10L, date.toString(), date.toString(), null);

        assertThat(response.days.get(0).slots().get(0).users())
                .extracting(worker -> worker.workStatusCode())
                .containsExactlyInAnyOrder("WK02", "WK03", "WK04");
    }

    @Test
    @DisplayName("관리자 근로시간표 조회 - 근무 전 출근 기록이 없으면 근무 예정 상태를 반환한다")
    void returnsScheduledStatusBeforeWork() {
        LocalDate date = LocalDate.now().plusDays(1);
        WorkScheduleSetting setting = setting();
        WorkSchedule scheduled = schedule(101L, 1L, "근무예정", date, setting);

        when(settingRepository.findByOrganizationIdAndYearAndMonth(
                10L, date.getYear(), date.getMonthValue()))
                .thenReturn(Optional.of(setting));
        when(scheduleRepository.findAllBySettingAndDateBetweenAndStatusCodeIn(
                setting, date, date, List.of(CodeType.WS01, CodeType.WS02)))
                .thenReturn(List.of(scheduled));
        when(unavailableTimeRepository.findBySettingAndDateBetween(setting, date, date))
                .thenReturn(List.of());
        when(attendanceRepository.findAllByScheduleIn(List.of(scheduled))).thenReturn(List.of());

        var response = service.getSchedules(10L, date.toString(), date.toString(), null);

        assertThat(response.days.get(0).slots().get(0).users().get(0).workStatusCode())
                .isEqualTo("WK01");
    }

    private WorkSchedule schedule(
            Long scheduleId,
            Long userId,
            String userName,
            LocalDate date,
            WorkScheduleSetting setting
    ) {
        return WorkSchedule.builder()
                .scheduleId(scheduleId)
                .setting(setting)
                .user(User.builder().userId(userId).name(userName).build())
                .date(date)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(9, 30))
                .statusCode(CodeType.WS02)
                .build();
    }

    private WorkAttendance attendance(WorkSchedule schedule, CodeType checkTypeCode) {
        return WorkAttendance.builder()
                .schedule(schedule)
                .user(schedule.getUser())
                .checkTypeCode(checkTypeCode)
                .checkTime(LocalDateTime.of(schedule.getDate(), schedule.getStartTime()))
                .build();
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
