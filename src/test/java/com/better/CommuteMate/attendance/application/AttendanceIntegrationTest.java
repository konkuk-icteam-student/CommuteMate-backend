package com.better.CommuteMate.attendance.application;

import com.better.CommuteMate.attendance.application.QrTokenManager;
import com.better.CommuteMate.attendance.application.WorkAttendanceService;
import com.better.CommuteMate.attendance.application.exception.AttendanceErrorCode;
import com.better.CommuteMate.attendance.application.exception.AttendanceException;
import com.better.CommuteMate.attendance.controller.dto.AttendanceHistoryResponse;
import com.better.CommuteMate.attendance.controller.dto.QrTokenResponse;
import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.global.code.CodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceIntegrationTest {

    @Mock
    private WorkAttendanceRepository workAttendanceRepository;
    @Mock
    private WorkSchedulesRepository workSchedulesRepository;
    @Mock
    private UserRepository userRepository;
    
    // Spy on real component to test interaction
    @Spy
    private QrTokenManager qrTokenManager = new QrTokenManager();

    @InjectMocks
    private WorkAttendanceService workAttendanceService;

    private User testUser;
    private WorkSchedule testSchedule;

    @BeforeEach
    void setUp() {
        testUser = User.builder().userId(1).name("Test User").build();
        LocalDateTime now = LocalDateTime.now();
        testSchedule = WorkSchedule.builder()
                .scheduleId(100)
                .user(testUser)
                .startTime(now.minusHours(1))
                .endTime(now.plusMinutes(1)) // Valid for both check-in and check-out
                .build();
    }

    @Test
    @DisplayName("출근부터 퇴근까지 전체 흐름 통합 테스트")
    void fullAttendanceFlow_Success() {
        // 1. Generate Token
        QrTokenResponse tokenResponse = workAttendanceService.generateQrToken();
        String token = tokenResponse.getToken();
        assertThat(token).isNotNull();

        // 2. Check In
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(workSchedulesRepository.findValidSchedulesByUserAndDateRange(anyInt(), any(), any()))
                .thenReturn(List.of(testSchedule));
        when(workAttendanceRepository.findBySchedule_ScheduleId(100)).thenReturn(Collections.emptyList());

        workAttendanceService.checkIn(1, token);
        
        verify(workAttendanceRepository, times(1)).save(argThat(a -> 
            a.getCheckTypeCode() == CodeType.CT01 && a.getSchedule().getScheduleId() == 100
        ));

        // 3. Check Out
        // Mock state: User is now checked in
        WorkAttendance checkInRecord = WorkAttendance.builder()
                .attendanceId(1)
                .user(testUser)
                .schedule(testSchedule)
                .checkTypeCode(CodeType.CT01)
                .checkTime(LocalDateTime.now())
                .build();
        
        when(workAttendanceRepository.findBySchedule_ScheduleId(100)).thenReturn(List.of(checkInRecord));
        
        // Generate new token for checkout (simulating time passing or fresh token)
        QrTokenResponse tokenResponse2 = workAttendanceService.generateQrToken();
        String token2 = tokenResponse2.getToken();

        workAttendanceService.checkOut(1, token2);

        verify(workAttendanceRepository, times(1)).save(argThat(a -> 
            a.getCheckTypeCode() == CodeType.CT02 && a.getSchedule().getScheduleId() == 100
        ));
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 체크인 시도 시 실패")
    void checkIn_InvalidToken_Flow() {
        // Generate a token but use a different one
        workAttendanceService.generateQrToken();
        
        assertThatThrownBy(() -> workAttendanceService.checkIn(1, "fake-token"))
                .isInstanceOf(AttendanceException.class)
                .hasMessage(AttendanceErrorCode.INVALID_QR_TOKEN.getMessage());
        
        verify(workAttendanceRepository, never()).save(any());
    }
}
