package com.better.CommuteMate.attendance.application;

import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.AttendanceErrorCode;
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
                .isInstanceOf(CustomException.class)
                .hasMessage(AttendanceErrorCode.INVALID_QR_TOKEN.getMessage());
    }

    @Test
    @DisplayName("출근 체크 - 근무 일정 없음 예외")
    void checkIn_NoSchedule() {
        when(qrTokenManager.validateToken("valid")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().userId(1L).build()));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(anyLong(), any(), any(), anyList()))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> workAttendanceService.checkIn(1L, "valid"))
                .isInstanceOf(CustomException.class)
                .hasMessage(AttendanceErrorCode.NO_SCHEDULE_FOUND.getMessage());
    }

    @Test
    @DisplayName("출근 체크 - 근무 시간 아님 (1시간 전)")
    void checkIn_NotWorkTime() {
        // Given
        // 자정 근처(now)에 offset을 더해도 자정을 넘지 않도록 30~90분 이내로 최소화한다.
        LocalDateTime now = LocalDateTime.now();
        WorkSchedule schedule = WorkSchedule.builder()
                .scheduleId(1L)
                .date(now.toLocalDate())
                .startTime(now.plusMinutes(30).toLocalTime()) // 30분 후 시작
                .endTime(now.plusMinutes(90).toLocalTime())
                .statusCode(CodeType.WS02)
                .build();

        when(qrTokenManager.validateToken("valid")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().userId(1L).build()));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(anyLong(), any(), any(), anyList()))
                .thenReturn(List.of(schedule));

        // When & Then
        assertThatThrownBy(() -> workAttendanceService.checkIn(1L, "valid"))
                .isInstanceOf(CustomException.class)
                .hasMessage(AttendanceErrorCode.NOT_WORK_TIME.getMessage());
    }

    @Test
    @DisplayName("출근 체크 - 이미 출근함 예외")
    void checkIn_AlreadyCheckedIn() {
        // Given
        // 자정 근처(now)에 offset을 더해도 자정을 넘지 않도록 종료 offset을 최소화한다.
        LocalDateTime now = LocalDateTime.now();
        WorkSchedule schedule = WorkSchedule.builder()
                .scheduleId(1L)
                .date(now.toLocalDate())
                .startTime(now.minusMinutes(5).toLocalTime()) // Started 5 mins ago
                .endTime(now.plusMinutes(25).toLocalTime())
                .statusCode(CodeType.WS02)
                .build();

        WorkAttendance existing = WorkAttendance.builder()
                .checkTypeCode(CodeType.CT01)
                .build();

        when(qrTokenManager.validateToken("valid")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().userId(1L).build()));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(anyLong(), any(), any(), anyList()))
                .thenReturn(List.of(schedule));
        when(workAttendanceRepository.findAllByScheduleIn(anyList())).thenReturn(List.of(existing));

        // When & Then
        assertThatThrownBy(() -> workAttendanceService.checkIn(1L, "valid"))
                .isInstanceOf(CustomException.class)
                .hasMessage(AttendanceErrorCode.ALREADY_CHECKED_IN.getMessage());
    }

    @Test
    @DisplayName("출근 체크 - 성공")
    void checkIn_Success() {
        // Given
        // 자정 근처(now)에 offset을 더해도 자정을 넘지 않도록 종료 offset을 최소화한다.
        LocalDateTime now = LocalDateTime.now();
        WorkSchedule schedule = WorkSchedule.builder()
                .scheduleId(1L)
                .date(now.toLocalDate())
                .startTime(now.minusMinutes(5).toLocalTime())
                .endTime(now.plusMinutes(25).toLocalTime())
                .statusCode(CodeType.WS02)
                .build();

        when(qrTokenManager.validateToken("valid")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().userId(1L).build()));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(anyLong(), any(), any(), anyList()))
                .thenReturn(List.of(schedule));
        when(workAttendanceRepository.findAllByScheduleIn(anyList())).thenReturn(Collections.emptyList());

        // When
        workAttendanceService.checkIn(1L, "valid");

        // Then
        verify(workAttendanceRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("퇴근 체크 - 출근 안함 예외")
    void checkOut_NotCheckedIn() {
        // Given
        // 자정 근처(now)에 offset을 빼도 자정을 넘지 않도록 시작 offset을 최소화한다.
        LocalDateTime now = LocalDateTime.now();
        WorkSchedule schedule = WorkSchedule.builder()
                .scheduleId(1L)
                .date(now.toLocalDate())
                .startTime(now.minusMinutes(40).toLocalTime())
                .endTime(now.minusMinutes(1).toLocalTime())
                .statusCode(CodeType.WS02)
                .build();

        when(qrTokenManager.validateToken("valid")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().userId(1L).build()));
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(anyLong(), any(), any(), anyList()))
                .thenReturn(List.of(schedule));
        // No attendance records
        when(workAttendanceRepository.findAllByScheduleIn(anyList())).thenReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> workAttendanceService.checkOut(1L, "valid"))
                .isInstanceOf(CustomException.class)
                .hasMessage(AttendanceErrorCode.CHECK_IN_REQUIRED.getMessage());
    }

    @Test
    @DisplayName("연속 슬롯 근무 - 첫 슬롯에 출근한 뒤 마지막 슬롯에서 퇴근할 수 있다")
    void checkOut_ConsecutiveSlots_SucceedsWithCheckInOnFirstSlot() {
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder().userId(1L).build();
        WorkSchedule firstSlot = WorkSchedule.builder()
                .scheduleId(1L)
                .user(user)
                .date(now.toLocalDate())
                .startTime(now.minusMinutes(58).toLocalTime())
                .endTime(now.minusMinutes(28).toLocalTime())
                .statusCode(CodeType.WS02)
                .build();
        WorkSchedule lastSlot = WorkSchedule.builder()
                .scheduleId(2L)
                .user(user)
                .date(now.toLocalDate())
                .startTime(now.minusMinutes(28).toLocalTime())
                .endTime(now.plusMinutes(2).toLocalTime())
                .statusCode(CodeType.WS02)
                .build();
        WorkAttendance checkIn = WorkAttendance.builder()
                .schedule(firstSlot)
                .checkTypeCode(CodeType.CT01)
                .checkTime(now.minusMinutes(58))
                .build();

        when(qrTokenManager.validateToken("valid")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        // Repository 반환 순서와 무관하게 연속 근무 묶음으로 처리되어야 한다.
        when(workSchedulesRepository.findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
                anyLong(), any(), any(), anyList()))
                .thenReturn(List.of(lastSlot, firstSlot));
        when(workAttendanceRepository.findAllByScheduleIn(anyList())).thenReturn(List.of(checkIn));

        assertThatCode(() -> workAttendanceService.checkOut(1L, "valid"))
                .doesNotThrowAnyException();
        verify(workAttendanceRepository).saveAll(argThat(attendances -> {
            List<WorkAttendance> saved = java.util.stream.StreamSupport
                    .stream(attendances.spliterator(), false)
                    .toList();
            return saved.size() == 2 && saved.stream()
                    .allMatch(attendance -> attendance.getCheckTypeCode() == CodeType.CT02);
        }));
    }
}
