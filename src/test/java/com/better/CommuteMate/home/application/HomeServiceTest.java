package com.better.CommuteMate.home.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.BasicException;
import com.better.CommuteMate.home.application.HomeService;
import com.better.CommuteMate.home.controller.dto.HomeAttendanceStatusResponse;
import com.better.CommuteMate.home.controller.dto.HomeAttendanceStatusResponse.AttendanceStatus;
import com.better.CommuteMate.home.controller.dto.HomeWorkTimeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
        when(userRepository.findById(anyInt())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> homeService.getTodayWorkTime(1))
                .isInstanceOf(BasicException.class);
    }

    @Test
    @DisplayName("오늘의 근무 시간 조회 - 스케줄이 있고 출퇴근 기록이 있는 경우 계산 확인")
    void getTodayWorkTime_Success() {
        // Given
        User user = User.builder().userId(1).build();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minusHours(4);
        LocalDateTime endTime = now.plusHours(4);

        WorkSchedule schedule = WorkSchedule.builder()
                .scheduleId(1)
                .startTime(startTime)
                .endTime(endTime)
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

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(anyInt(), any(), any()))
                .thenReturn(new java.util.ArrayList<>(List.of(schedule)));
        when(workAttendanceRepository.findBySchedule_ScheduleId(1))
                .thenReturn(List.of(checkIn, checkOut));

        // When
        HomeWorkTimeResponse response = homeService.getTodayWorkTime(1);

        // Then
        assertThat(response.getScheduleCount()).isEqualTo(1);
        assertThat(response.getTotalMinutes()).isEqualTo(180); // 3 hours
    }

    @Test
    @DisplayName("출퇴근 상태 조회 - 스케줄 없음")
    void getAttendanceStatus_NoSchedule() {
        User user = User.builder().userId(1).build();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(anyInt(), any(), any()))
                .thenReturn(new java.util.ArrayList<>(Collections.emptyList()));

        HomeAttendanceStatusResponse response = homeService.getAttendanceStatus(1);

        assertThat(response.getStatus()).isEqualTo(AttendanceStatus.NO_SCHEDULE);
    }

    @Test
    @DisplayName("출퇴근 상태 조회 - 출근 전 (10분 이상 남음)")
    void getAttendanceStatus_BeforeWork() {
        User user = User.builder().userId(1).build();
        LocalDateTime now = LocalDateTime.now();
        // Starts in 1 hour
        WorkSchedule schedule = WorkSchedule.builder()
                .scheduleId(1)
                .startTime(now.plusHours(1))
                .endTime(now.plusHours(4))
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(anyInt(), any(), any()))
                .thenReturn(new java.util.ArrayList<>(List.of(schedule)));
        when(workAttendanceRepository.findBySchedule_ScheduleId(1))
                .thenReturn(Collections.emptyList());

        HomeAttendanceStatusResponse response = homeService.getAttendanceStatus(1);

        assertThat(response.getStatus()).isEqualTo(AttendanceStatus.BEFORE_WORK);
    }
}
