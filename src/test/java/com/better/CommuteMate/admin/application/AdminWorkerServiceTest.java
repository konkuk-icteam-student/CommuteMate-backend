package com.better.CommuteMate.admin.application;

import com.better.CommuteMate.admin.controller.dto.AdminWorkerDetailResponse;
import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.repository.WorkScheduleSettingRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.entity.UserProfile;
import com.better.CommuteMate.domain.user.repository.UserProfileRepository;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestRepository;
import com.better.CommuteMate.global.code.CodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminWorkerServiceTest {

    private static final Long ORG_ID = 10L;
    private static final Long USER_ID = 1L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private WorkSchedulesRepository scheduleRepository;
    @Mock
    private WorkAttendanceRepository attendanceRepository;
    @Mock
    private WorkScheduleSettingRepository settingRepository;
    @Mock
    private WorkChangeRequestRepository changeRequestRepository;

    private AdminWorkerService service;

    @BeforeEach
    void setUp() {
        service = new AdminWorkerService(
                userRepository, userProfileRepository, scheduleRepository,
                attendanceRepository, settingRepository, changeRequestRepository
        );
    }

    private User sampleUser() {
        return User.builder().userId(USER_ID).organizationId(ORG_ID).name("홍길동")
                .email("test@test.com").roleCode(CodeType.RL01).build();
    }

    private void stubCommon(User user, UserProfile profile, int year, int month, List<WorkSchedule> ws02Schedules) {
        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(settingRepository.findByOrganizationIdAndYearAndMonth(ORG_ID, year, month))
                .thenReturn(Optional.of(WorkScheduleSetting.builder()
                        .weeklyMaxMinutes(780).monthlyMaxMinutes(1620).build()));
        when(userProfileRepository.findAllByUserIdIn(List.of(USER_ID)))
                .thenReturn(profile == null ? List.of() : List.of(profile));
        when(scheduleRepository.findAllByUser_UserIdInAndDateBetweenAndStatusCode(
                eq(List.of(USER_ID)), any(), any(), eq(CodeType.WS02)))
                .thenReturn(ws02Schedules);
        lenient().when(attendanceRepository.findAllByScheduleIn(anyList())).thenReturn(List.of());
        when(changeRequestRepository.countByUser_UserId(USER_ID)).thenReturn(0L);
        when(changeRequestRepository.countByUser_UserIdAndStatusCode(USER_ID, CodeType.CS02)).thenReturn(0L);
    }

    private WorkSchedule ws02(LocalDate date, LocalTime start, LocalTime end) {
        return WorkSchedule.builder()
                .date(date).startTime(start).endTime(end).statusCode(CodeType.WS02).build();
    }

    @Test
    void submittedMinutes_sumsWs02SlotDurationsInQueriedMonth() {
        // 30분 슬롯 54개(18일 x 3슬롯) = 1620분(27시간), 모두 7월 안에 있음
        List<WorkSchedule> schedules = new ArrayList<>();
        for (int day = 0; day < 18; day++) {
            for (int slot = 0; slot < 3; slot++) {
                LocalTime start = LocalTime.of(9 + slot, 0);
                schedules.add(ws02(LocalDate.of(2026, 7, 1).plusDays(day), start, start.plusMinutes(30)));
            }
        }
        stubCommon(sampleUser(), null, 2026, 7, schedules);

        AdminWorkerDetailResponse response = service.getWorker(ORG_ID, USER_ID, "2026-07-15");

        assertThat(response.submittedMinutes).isEqualTo(1620L);
    }

    @Test
    void submittedMinutes_zeroWhenNoRequests() {
        stubCommon(sampleUser(), null, 2026, 7, List.of());

        AdminWorkerDetailResponse response = service.getWorker(ORG_ID, USER_ID, "2026-07-15");

        assertThat(response.submittedMinutes).isEqualTo(0L);
    }

    @Test
    void submittedMinutes_excludesSlotsOutsideQueriedMonth() {
        // 기준일 2026-07-01(수)의 주간 범위는 6/29(월)~7/5(일)이라 6월 말 슬롯도 조회되지만
        // 총 제출시간은 조회 "월"(7월) 슬롯만 합산해야 한다.
        LocalDate juneSlotDate = LocalDate.of(2026, 6, 29);
        LocalDate julySlotDate = LocalDate.of(2026, 7, 1);
        List<WorkSchedule> schedules = List.of(
                ws02(juneSlotDate, LocalTime.of(9, 0), LocalTime.of(11, 0)), // 6월, 제외 대상 120분
                ws02(julySlotDate, LocalTime.of(9, 0), LocalTime.of(9, 30))  // 7월, 포함 대상 30분
        );
        stubCommon(sampleUser(), null, 2026, 7, schedules);

        AdminWorkerDetailResponse response = service.getWorker(ORG_ID, USER_ID, "2026-07-01");

        assertThat(response.submittedMinutes).isEqualTo(30L);
    }

    @Test
    void submittedMinutes_reusesExistingWs02QueryWithoutDuplicateFetch() {
        stubCommon(sampleUser(), null, 2026, 7, List.of());

        service.getWorker(ORG_ID, USER_ID, "2026-07-15");

        verify(scheduleRepository, times(1)).findAllByUser_UserIdInAndDateBetweenAndStatusCode(
                eq(List.of(USER_ID)), any(), any(), eq(CodeType.WS02));
    }

    @Test
    void existingFields_regressionCheck() {
        User user = sampleUser();
        UserProfile profile = UserProfile.builder().userId(USER_ID).user(user)
                .studentId("202211414").department("컴퓨터공학과").grade(3).phoneNumber("010-1234-5678").build();
        stubCommon(user, profile, 2026, 7, List.of());

        AdminWorkerDetailResponse response = service.getWorker(ORG_ID, USER_ID, "2026-07-15");

        assertThat(response.userId).isEqualTo(USER_ID);
        assertThat(response.name).isEqualTo("홍길동");
        assertThat(response.studentId).isEqualTo("202211414");
        assertThat(response.department).isEqualTo("컴퓨터공학과");
        assertThat(response.grade).isEqualTo(3);
        assertThat(response.phoneNumber).isEqualTo("010-1234-5678");
        assertThat(response.email).isEqualTo("test@test.com");
        assertThat(response.weeklyLimitMinutes).isEqualTo(780);
        assertThat(response.monthlyLimitMinutes).isEqualTo(1620);
        assertThat(response.submittedMinutes).isEqualTo(0L);
    }
}
