package com.better.CommuteMate.home.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.home.controller.dto.HomeAttendanceStatusResponse;
import com.better.CommuteMate.home.controller.dto.HomeAttendanceStatusResponse.AttendanceStatus;
import com.better.CommuteMate.home.controller.dto.HomeCheckInResponse;
import com.better.CommuteMate.home.controller.dto.HomeWorkTimeResponse;
import com.better.CommuteMate.home.controller.dto.WeeklyWorkSummaryResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock
    private WorkSchedulesRepository workSchedulesRepository;

    @Mock
    private WorkAttendanceRepository workAttendanceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private HomeService homeService;

    @Test
    @DisplayName("오늘의 근무 시간 조회 - 사용자가 없는 경우 예외 발생")
    void getTodayWorkTime_UserNotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> homeService.getTodayWorkTime(1L))
                .isInstanceOf(CustomException.class);
    }

    @Disabled("임시 비활성화")
    @Test
    @DisplayName("오늘의 근무 시간 조회 - 스케줄이 있고 출퇴근 기록이 있는 경우 계산 확인")
    void getTodayWorkTime_Success() {
        // Given
        User user = User.builder().userId(1L).build();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minusHours(4);
        LocalDateTime endTime = now.plusHours(4);

        WorkSchedule schedule = WorkSchedule.builder()
                .scheduleId(1L)
                .date(now.toLocalDate())
                .startTime(startTime.toLocalTime())
                .endTime(endTime.toLocalTime())
                .statusCode(CodeType.WS02)
                .build();

        // 3 hours worked
        WorkAttendance checkIn = WorkAttendance.builder()
                .checkTypeCode(CodeType.CT01)
                .checkTime(startTime)
                .build();
        WorkAttendance checkOut = WorkAttendance.builder()
                .checkTypeCode(CodeType.CT02)
                .checkTime(startTime.plusHours(3))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(anyLong(), any(), any(), anyList()))
                .thenReturn(new java.util.ArrayList<>(List.of(schedule)));
        when(workAttendanceRepository.findBySchedule_ScheduleId(1L))
                .thenReturn(List.of(checkIn, checkOut));

        // When
        HomeWorkTimeResponse response = homeService.getTodayWorkTime(1L);

        // Then
        assertThat(response.getScheduleCount()).isEqualTo(1);
        assertThat(response.getTotalMinutes()).isEqualTo(180); // 3 hours
    }

    @Test
    @DisplayName("출퇴근 상태 조회 - 스케줄 없음")
    void getAttendanceStatus_NoSchedule() {
        User user = User.builder().userId(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(anyLong(), any(), any(), anyList()))
                .thenReturn(new java.util.ArrayList<>(Collections.emptyList()));

        HomeAttendanceStatusResponse response = homeService.getAttendanceStatus(1L);

        assertThat(response.getStatus()).isEqualTo(AttendanceStatus.NO_SCHEDULE);
    }

    @Test
    @DisplayName("출퇴근 상태 조회 - 출근 전 (10분 이상 남음)")
    void getAttendanceStatus_BeforeWork() {
        User user = User.builder().userId(1L).build();
        LocalDateTime now = LocalDateTime.now();
        // Starts in 1 hour
        WorkSchedule schedule = WorkSchedule.builder()
                .scheduleId(1L)
                .date(now.toLocalDate())
                .startTime(now.plusHours(1).toLocalTime())
                .endTime(now.plusHours(4).toLocalTime())
                .statusCode(CodeType.WS02)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(anyLong(), any(), any(), anyList()))
                .thenReturn(new java.util.ArrayList<>(List.of(schedule)));
        when(workAttendanceRepository.findBySchedule_ScheduleId(1L))
                .thenReturn(Collections.emptyList());

        HomeAttendanceStatusResponse response = homeService.getAttendanceStatus(1L);

        assertThat(response.getStatus()).isEqualTo(AttendanceStatus.BEFORE_WORK);
    }

    @Test
    @DisplayName("연속 슬롯 근무 - 마지막 슬롯의 퇴근 기록으로 전체 근무시간을 완료 처리한다")
    void getWorkSummary_CheckOutOnLastSlot_CompletesEntireConsecutiveWork() {
        User user = User.builder().userId(1L).build();
        LocalDate today = LocalDate.now();
        List<WorkSchedule> slots = List.of(
                schedule(1L, user, today, 9, 0, 9, 30),
                schedule(2L, user, today, 9, 30, 10, 0),
                schedule(3L, user, today, 10, 0, 10, 30),
                schedule(4L, user, today, 10, 30, 11, 0)
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
                anyLong(), any(), any(), anyList())).thenReturn(slots);
        for (WorkSchedule slot : slots) {
            WorkAttendance checkIn = WorkAttendance.builder()
                    .schedule(slot)
                    .checkTypeCode(CodeType.CT01)
                    .checkTime(today.atTime(9, 0))
                    .build();
            if (slot.getScheduleId().equals(4L)) {
                WorkAttendance checkOut = WorkAttendance.builder()
                        .schedule(slot)
                        .checkTypeCode(CodeType.CT02)
                        .checkTime(today.atTime(11, 0))
                        .build();
                when(workAttendanceRepository.findBySchedule_ScheduleId(4L))
                        .thenReturn(List.of(checkIn, checkOut));
            } else {
                when(workAttendanceRepository.findBySchedule_ScheduleId(slot.getScheduleId()))
                        .thenReturn(List.of(checkIn));
            }
        }

        WeeklyWorkSummaryResponse response = homeService.getWorkSummary(1L);

        assertThat(response.getTotalWeeklyHours()).isEqualTo(2.0);
        assertThat(response.getCompletedWeeklyHours()).isEqualTo(2.0);
        assertThat(response.getCompletedMonthlyHours()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("체크인 응답의 checkInTime은 KST(+9h)로 보정되지만, 저장되는 WorkAttendance.checkTime과 지각 판정은 UTC(now()) 그대로다")
    void checkIn_appliesKstOffsetOnlyToResponseCheckInTime() {
        User user = User.builder().userId(1L).build();
        LocalDateTime beforeCall = LocalDateTime.now();
        WorkSchedule schedule = WorkSchedule.builder()
                .scheduleId(1L)
                .user(user)
                .date(beforeCall.toLocalDate())
                .startTime(beforeCall.toLocalTime())
                .endTime(beforeCall.toLocalTime().plusHours(1))
                .statusCode(CodeType.WS02)
                .build();

        when(workSchedulesRepository.findAllById(List.of(1L)))
                .thenReturn(new java.util.ArrayList<>(List.of(schedule)));
        when(workAttendanceRepository.findAllByScheduleIn(List.of(schedule))).thenReturn(List.of());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(workAttendanceRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        HomeCheckInResponse response = homeService.checkIn(1L, List.of(1L));
        LocalDateTime afterCall = LocalDateTime.now();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<WorkAttendance>> captor = ArgumentCaptor.forClass(List.class);
        verify(workAttendanceRepository).saveAll(captor.capture());
        LocalDateTime storedCheckTime = captor.getValue().get(0).getCheckTime();

        // 저장값(checkTime)은 now()(UTC) 그대로 — 보정되지 않아야 한다
        assertThat(storedCheckTime).isBetween(beforeCall, afterCall);
        // 응답의 checkInTime만 저장값보다 정확히 9시간 앞서 있어야 한다(보정)
        assertThat(response.getCheckInTime()).isEqualTo(storedCheckTime.plusHours(9));
    }

    private WorkSchedule schedule(Long id, User user, LocalDate date,
                                  int startHour, int startMinute, int endHour, int endMinute) {
        return WorkSchedule.builder()
                .scheduleId(id)
                .user(user)
                .date(date)
                .startTime(LocalTime.of(startHour, startMinute))
                .endTime(LocalTime.of(endHour, endMinute))
                .statusCode(CodeType.WS02)
                .build();
    }
}
