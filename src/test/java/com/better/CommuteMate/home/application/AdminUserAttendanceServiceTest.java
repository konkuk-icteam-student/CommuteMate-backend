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
                .scheduleId("schedule-1")
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
                "10", date.getYear(), date.getMonthValue()
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
