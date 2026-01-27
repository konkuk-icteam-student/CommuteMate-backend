package com.better.CommuteMate.attendance.application;

import com.better.CommuteMate.attendance.application.QrTokenManager;
import com.better.CommuteMate.attendance.application.WorkAttendanceService;
import com.better.CommuteMate.attendance.application.exception.AttendanceErrorCode;
import com.better.CommuteMate.attendance.application.exception.AttendanceException;
import com.better.CommuteMate.attendance.controller.dto.QrTokenResponse;
import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.global.code.CodeType;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkAttendanceServiceTest {

    @Mock
    private WorkAttendanceRepository workAttendanceRepository;
    @Mock
    private WorkSchedulesRepository workSchedulesRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private QrTokenManager qrTokenManager;

    @InjectMocks
    private WorkAttendanceService workAttendanceService;

    @Test
    @DisplayName("QR 토큰 발급 성공")
    void generateQrToken_Success() {
        when(qrTokenManager.generateToken()).thenReturn("test-token");
        
        QrTokenResponse response = workAttendanceService.generateQrToken();
        
        assertThat(response.getToken()).isEqualTo("test-token");
        assertThat(response.getValidSeconds()).isEqualTo(60);
    }

    @Test
    @DisplayName("출근 체크 - 유효하지 않은 토큰시 예외 발생")
    void checkIn_InvalidToken() {
        when(qrTokenManager.validateToken("invalid")).thenReturn(false);
        
        assertThatThrownBy(() -> workAttendanceService.checkIn(1L, "invalid"))
                .isInstanceOf(AttendanceException.class)
                .hasMessage(AttendanceErrorCode.INVALID_QR_TOKEN.getMessage());
    }

    @Test
    @DisplayName("출근 체크 - 근무 일정 없음 예외")
    void checkIn_NoSchedule() {
        when(qrTokenManager.validateToken("valid")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().userId(1L).build()));
        when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> workAttendanceService.checkIn(1L, "valid"))
                .isInstanceOf(AttendanceException.class)
                .hasMessage(AttendanceErrorCode.NO_SCHEDULE_FOUND.getMessage());
    }

    @Test
    @DisplayName("출근 체크 - 근무 시간 아님 (1시간 전)")
    void checkIn_NotWorkTime() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        WorkSchedule schedule = WorkSchedule.builder()
                .scheduleId(1L)
                .startTime(now.plusHours(1)) // 1 hour later
                .endTime(now.plusHours(4))
                .build();

        when(qrTokenManager.validateToken("valid")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().userId(1L).build()));
        when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(anyLong(), any(), any()))
                .thenReturn(List.of(schedule));

        // When & Then
        assertThatThrownBy(() -> workAttendanceService.checkIn(1L, "valid"))
                .isInstanceOf(AttendanceException.class)
                .hasMessage(AttendanceErrorCode.NOT_WORK_TIME.getMessage());
    }

    @Test
    @DisplayName("출근 체크 - 이미 출근함 예외")
    void checkIn_AlreadyCheckedIn() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        WorkSchedule schedule = WorkSchedule.builder()
                .scheduleId(1L)
                .startTime(now.minusMinutes(5)) // Started 5 mins ago
                .endTime(now.plusHours(3))
                .build();
        
        WorkAttendance existing = WorkAttendance.builder()
                .checkTypeCode(CodeType.CT01)
                .build();

        when(qrTokenManager.validateToken("valid")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().userId(1L).build()));
        when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(anyLong(), any(), any()))
                .thenReturn(List.of(schedule));
        when(workAttendanceRepository.findBySchedule_ScheduleId(1L)).thenReturn(List.of(existing));

        // When & Then
        assertThatThrownBy(() -> workAttendanceService.checkIn(1L, "valid"))
                .isInstanceOf(AttendanceException.class)
                .hasMessage(AttendanceErrorCode.ALREADY_CHECKED_IN.getMessage());
    }

    @Test
    @DisplayName("출근 체크 - 성공")
    void checkIn_Success() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        WorkSchedule schedule = WorkSchedule.builder()
                .scheduleId(1L)
                .startTime(now.minusMinutes(5))
                .endTime(now.plusHours(3))
                .build();

        when(qrTokenManager.validateToken("valid")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().userId(1L).build()));
        when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(anyLong(), any(), any()))
                .thenReturn(List.of(schedule));
        when(workAttendanceRepository.findBySchedule_ScheduleId(1L)).thenReturn(Collections.emptyList());

        // When
        workAttendanceService.checkIn(1L, "valid");

        // Then
        verify(workAttendanceRepository).save(any(WorkAttendance.class));
    }

    @Test
    @DisplayName("퇴근 체크 - 출근 안함 예외")
    void checkOut_NotCheckedIn() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        WorkSchedule schedule = WorkSchedule.builder()
                .scheduleId(1L)
                .startTime(now.minusHours(3))
                .endTime(now.plusMinutes(5)) // Ends in 5 mins
                .build();

        when(qrTokenManager.validateToken("valid")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().userId(1L).build()));
        when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(anyLong(), any(), any()))
                .thenReturn(List.of(schedule));
        // No attendance records
        when(workAttendanceRepository.findBySchedule_ScheduleId(1L)).thenReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> workAttendanceService.checkOut(1L, "valid"))
                .isInstanceOf(AttendanceException.class)
                .hasMessage(AttendanceErrorCode.CHECK_IN_REQUIRED.getMessage());
    }
}
