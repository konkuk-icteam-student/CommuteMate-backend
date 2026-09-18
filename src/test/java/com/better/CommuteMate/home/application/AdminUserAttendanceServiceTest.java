package com.better.CommuteMate.home.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.repository.WorkScheduleSettingRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.entity.UserProfile;
import com.better.CommuteMate.domain.user.repository.UserProfileRepository;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.home.controller.dto.AdminUserAttendancePageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserAttendanceServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserProfileRepository userProfileRepository;
    @Mock WorkSchedulesRepository scheduleRepository;
    @Mock WorkAttendanceRepository attendanceRepository;
    @Mock WorkScheduleSettingRepository settingRepository;

    AdminUserAttendanceService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserAttendanceService(
                userRepository,
                userProfileRepository,
                scheduleRepository,
                attendanceRepository,
                settingRepository
        );
    }

    @Test
    void returnsWorkingAndLateCodesWithProfileAndPagination() {
        LocalDate date = LocalDate.now();
        LocalTime start = LocalTime.now().minusMinutes(30).withSecond(0).withNano(0);
        LocalTime end = LocalTime.now().plusMinutes(30).withSecond(0).withNano(0);
        User user = User.builder()
                .userId(1L)
                .organizationId(10L)
                .name("최지훈")
                .roleCode(CodeType.RL01)
                .build();
        UserProfile profile = UserProfile.builder()
                .userId(1L)
                .user(user)
                .department("정보보호학부")
                .studentId("202311306")
                .grade(2)
                .phoneNumber("010-0000-0000")
                .build();
        WorkSchedule schedule = WorkSchedule.builder()
                .scheduleId(1L)
                .user(user)
                .date(date)
                .startTime(start)
                .endTime(end)
                .statusCode(CodeType.WS02)
                .build();
        WorkAttendance checkIn = WorkAttendance.builder()
                .schedule(schedule)
                .user(user)
                .checkTypeCode(CodeType.CT01)
                .checkTime(LocalDateTime.of(date, start.plusMinutes(11)))
                .build();
        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .weeklyMaxMinutes(540)
                .monthlyMaxMinutes(1620)
                .build();
        PageRequest pageable = PageRequest.of(0, 6);

        when(userRepository.findAllByOrganizationIdAndRoleCodeAndNameContainingIgnoreCase(
                10L, CodeType.RL01, "", pageable
        )).thenReturn(new PageImpl<>(List.of(user), pageable, 1));
        when(userProfileRepository.findAllByUserIdIn(List.of(1L)))
                .thenReturn(List.of(profile));
        when(scheduleRepository.findAllByUser_UserIdInAndDateBetweenAndStatusCode(
                List.of(1L), date.withDayOfMonth(1), date.withDayOfMonth(date.lengthOfMonth()), CodeType.WS02
        )).thenReturn(List.of(schedule));
        when(attendanceRepository.findAllByScheduleIn(List.of(schedule)))
                .thenReturn(List.of(checkIn));
        when(settingRepository.findByOrganizationIdAndYearAndMonth(
                10L, date.getYear(), date.getMonthValue()
        )).thenReturn(Optional.of(setting));

        var response = service.getUserAttendance(10L, date.toString(), null, null, null);

        assertThat(response.page).isZero();
        assertThat(response.size).isEqualTo(6);
        assertThat(response.totalElements).isEqualTo(1);
        assertThat(response.users).hasSize(1);
        assertThat(response.users.get(0).department()).isEqualTo("정보보호학부");
        assertThat(response.users.get(0).workStatusCode()).isEqualTo("WK02");
        assertThat(response.users.get(0).attendanceStatusCode()).isEqualTo("AT02");
        assertThat(response.users.get(0).lateCount()).isEqualTo(1);
        assertThat(response.users.get(0).lateMinutes()).isEqualTo(11);
        assertThat(response.users.get(0).weeklyLimitMinutes()).isEqualTo(540);
        assertThat(response.users.get(0).monthlyLimitMinutes()).isEqualTo(1620);
    }

    @Test
    void checkedInOnly_beforeEndTime_staysWorking() {
        LocalTime now = LocalTime.now().withNano(0);
        LocalTime start = now.minusMinutes(30);
        LocalTime end = now.plusMinutes(30);
        WorkSchedule schedule = scheduleOf(start, end);
        WorkAttendance checkIn = attendanceOf(schedule, CodeType.CT01, start);

        var user = respondUser(schedule, List.of(checkIn));

        assertThat(user.workStatusCode()).isEqualTo("WK02");
        assertThat(user.attendanceStatusCode()).isEqualTo("AT01");
        assertThat(user.weeklyWorkedMinutes()).isGreaterThanOrEqualTo(30);
    }

    @Test
    void checkedInOnly_afterEndTimeWithoutCheckOut_completesAndCapsWorkedMinutes() {
        LocalTime now = LocalTime.now().withNano(0);
        LocalTime start = now.minusMinutes(60);
        LocalTime end = now.minusMinutes(30);
        WorkSchedule schedule = scheduleOf(start, end);
        WorkAttendance checkIn = attendanceOf(schedule, CodeType.CT01, start);

        var user = respondUser(schedule, List.of(checkIn));

        assertThat(user.workStatusCode()).isEqualTo("WK03");
        assertThat(user.attendanceStatusCode()).isEqualTo("AT01");
        assertThat(user.weeklyWorkedMinutes()).isEqualTo(30);
        assertThat(user.monthlyWorkedMinutes()).isEqualTo(30);
    }

    @Test
    void checkedOut_alwaysCompletesUsingActualCheckOutTime() {
        LocalTime now = LocalTime.now().withNano(0);
        LocalTime start = now.minusMinutes(60);
        LocalTime end = now.minusMinutes(10);
        WorkSchedule schedule = scheduleOf(start, end);
        WorkAttendance checkIn = attendanceOf(schedule, CodeType.CT01, start);
        WorkAttendance checkOut = attendanceOf(schedule, CodeType.CT02, now.minusMinutes(20));

        var user = respondUser(schedule, List.of(checkIn, checkOut));

        assertThat(user.workStatusCode()).isEqualTo("WK03");
        assertThat(user.attendanceStatusCode()).isEqualTo("AT01");
        assertThat(user.weeklyWorkedMinutes()).isEqualTo(40);
        assertThat(user.monthlyWorkedMinutes()).isEqualTo(40);
    }

    @Test
    void noCheckIn_afterEndTime_staysAbsentWithZeroWorkedMinutes() {
        LocalTime now = LocalTime.now().withNano(0);
        LocalTime start = now.minusMinutes(60);
        LocalTime end = now.minusMinutes(30);
        WorkSchedule schedule = scheduleOf(start, end);

        var user = respondUser(schedule, List.of());

        assertThat(user.workStatusCode()).isEqualTo("WK04");
        assertThat(user.attendanceStatusCode()).isEqualTo("AT03");
        assertThat(user.weeklyWorkedMinutes()).isZero();
        assertThat(user.monthlyWorkedMinutes()).isZero();
    }

    private WorkSchedule scheduleOf(LocalTime start, LocalTime end) {
        User user = User.builder()
                .userId(1L)
                .organizationId(10L)
                .name("최지훈")
                .roleCode(CodeType.RL01)
                .build();
        return WorkSchedule.builder()
                .scheduleId(1L)
                .user(user)
                .date(LocalDate.now())
                .startTime(start)
                .endTime(end)
                .statusCode(CodeType.WS02)
                .build();
    }

    private WorkAttendance attendanceOf(WorkSchedule schedule, CodeType checkTypeCode, LocalTime time) {
        return WorkAttendance.builder()
                .schedule(schedule)
                .user(schedule.getUser())
                .checkTypeCode(checkTypeCode)
                .checkTime(LocalDateTime.of(schedule.getDate(), time))
                .build();
    }

    private AdminUserAttendancePageResponse.UserAttendance respondUser(
            WorkSchedule schedule, List<WorkAttendance> attendances
    ) {
        LocalDate date = schedule.getDate();
        User user = schedule.getUser();
        PageRequest pageable = PageRequest.of(0, 6);
        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .weeklyMaxMinutes(540)
                .monthlyMaxMinutes(1620)
                .build();

        when(userRepository.findAllByOrganizationIdAndRoleCodeAndNameContainingIgnoreCase(
                10L, CodeType.RL01, "", pageable
        )).thenReturn(new PageImpl<>(List.of(user), pageable, 1));
        when(userProfileRepository.findAllByUserIdIn(List.of(1L)))
                .thenReturn(List.of());
        when(scheduleRepository.findAllByUser_UserIdInAndDateBetweenAndStatusCode(
                List.of(1L), date.withDayOfMonth(1), date.withDayOfMonth(date.lengthOfMonth()), CodeType.WS02
        )).thenReturn(List.of(schedule));
        when(attendanceRepository.findAllByScheduleIn(List.of(schedule)))
                .thenReturn(attendances);
        when(settingRepository.findByOrganizationIdAndYearAndMonth(
                10L, date.getYear(), date.getMonthValue()
        )).thenReturn(Optional.of(setting));

        var response = service.getUserAttendance(10L, date.toString(), null, null, null);
        assertThat(response.users).hasSize(1);
        return response.users.get(0);
    }

    @Test
    void rejectsInvalidPage() {
        assertThatThrownBy(() ->
                service.getUserAttendance(10L, "2026-04-15", null, -1, 6)
        )
                .isInstanceOf(CustomException.class)
                .hasMessage("페이지 요청 값이 올바르지 않습니다.");
    }

    @Test
    void rejectsInvalidDate() {
        assertThatThrownBy(() ->
                service.getUserAttendance(10L, "invalid", null, 0, 6)
        )
                .isInstanceOf(CustomException.class)
                .hasMessage("조회 날짜 값이 올바르지 않습니다.");
    }
}
