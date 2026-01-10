package com.better.CommuteMate.application.schedule;

import com.better.CommuteMate.schedule.application.ScheduleValidator;
import com.better.CommuteMate.schedule.application.dtos.WorkScheduleCommand;
import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.MonthlyScheduleConfigRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.BasicException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleValidatorTest {

    @Mock
    private WorkSchedulesRepository workSchedulesRepository;

    @Mock
    private MonthlyScheduleConfigRepository monthlyScheduleConfigRepository;

    @InjectMocks
    private ScheduleValidator scheduleValidator;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduleValidator, "DEFAULT_MAX_CONCURRENT_SCHEDULES", 3);

        testUser = User.builder()
                .userId(1)
                .name("Test User")
                .build();
    }

    @Test
    @DisplayName("해당 시간대에 겹치는 일정이 없어서 등록 가능한 경우")
    void isScheduleInsertable_NoOverlapping_ReturnsTrue() {
        // Given
        WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 11, 0)
        );

        when(workSchedulesRepository.findByDate(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());

        // When
        boolean result = scheduleValidator.isScheduleInsertable(newSchedule);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("해당 시간대에 겹치는 일정이 최대 허용치 미만이어서 등록 가능한 경우")
    void isScheduleInsertable_OverlappingBelowLimit_ReturnsTrue() {
        // Given
        WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 11, 0)
        );

        List<WorkSchedule> existingSchedules = List.of(
                createWorkSchedule(LocalTime.of(8, 30), LocalTime.of(10, 0)),
                createWorkSchedule(LocalTime.of(10, 30), LocalTime.of(12, 0))
        );

        when(workSchedulesRepository.findByDate(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(existingSchedules);

        // When
        boolean result = scheduleValidator.isScheduleInsertable(newSchedule);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("해당 시간대에 겹치는 일정이 최대 허용치를 초과하여 등록 불가능한 경우")
    void isScheduleInsertable_OverlappingExceedsLimit_ReturnsFalse() {
        // Given
        WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 11, 0)
        );

        List<WorkSchedule> existingSchedules = List.of(
                createWorkSchedule(LocalTime.of(8, 0), LocalTime.of(10, 0)),
                createWorkSchedule(LocalTime.of(8, 30), LocalTime.of(10, 30)),
                createWorkSchedule(LocalTime.of(9, 30), LocalTime.of(11, 30))
        );

        when(workSchedulesRepository.findByDate(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(existingSchedules);

        // When
        boolean result = scheduleValidator.isScheduleInsertable(newSchedule);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("긴 시간대 일정에서 중간 체크포인트가 최대 허용치를 초과하는 경우")
    void isScheduleInsertable_LongScheduleWithMiddleOverlap_ReturnsFalse() {
        // Given
        WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 13, 0)
        );

        List<WorkSchedule> existingSchedules = List.of(
                createWorkSchedule(LocalTime.of(10, 0), LocalTime.of(12, 0)),
                createWorkSchedule(LocalTime.of(10, 30), LocalTime.of(12, 30)),
                createWorkSchedule(LocalTime.of(11, 0), LocalTime.of(13, 0))
        );

        when(workSchedulesRepository.findByDate(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(existingSchedules);

        // When
        boolean result = scheduleValidator.isScheduleInsertable(newSchedule);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("시작 시간과 끝 시간이 동일한 경우")
    void isScheduleInsertable_SameStartAndEndTime_ReturnsTrue() {
        // Given
        WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 9, 0)
        );

        when(workSchedulesRepository.findByDate(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());

        // When
        boolean result = scheduleValidator.isScheduleInsertable(newSchedule);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("30분 단위 체크포인트에서 겹치는 일정 확인")
    void isScheduleInsertable_ThirtyMinuteCheckpoints() {
        // Given
        WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 10, 0)
        );

        // 9:15와 9:45 체크포인트에서 3개씩 겹치도록 설정
        List<WorkSchedule> existingSchedules = List.of(
                createWorkSchedule(LocalTime.of(9, 0), LocalTime.of(9, 30)),  // 9:15에 겹침
                createWorkSchedule(LocalTime.of(9, 10), LocalTime.of(9, 40)), // 9:15에 겹침
                createWorkSchedule(LocalTime.of(9, 5), LocalTime.of(9, 25)),  // 9:15에 겹침
                createWorkSchedule(LocalTime.of(9, 30), LocalTime.of(10, 0)), // 9:45에 겹침
                createWorkSchedule(LocalTime.of(9, 40), LocalTime.of(10, 10)), // 9:45에 겹침
                createWorkSchedule(LocalTime.of(9, 35), LocalTime.of(9, 55))  // 9:45에 겹침
        );

        when(workSchedulesRepository.findByDate(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(existingSchedules);

        // When
        boolean result = scheduleValidator.isScheduleInsertable(newSchedule);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("경계값 테스트 - 정확히 최대 허용치와 같은 경우")
    void isScheduleInsertable_ExactlyAtLimit_ReturnsFalse() {
        // Given
        WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 10, 0)
        );

        // 9:15 체크포인트에서 정확히 3개가 겹치도록 설정
        List<WorkSchedule> existingSchedules = List.of(
                createWorkSchedule(LocalTime.of(8, 30), LocalTime.of(9, 30)), // 9:15에 겹침
                createWorkSchedule(LocalTime.of(9, 0), LocalTime.of(9, 45)),   // 9:15에 겹침
                createWorkSchedule(LocalTime.of(9, 10), LocalTime.of(9, 20))  // 9:15에 겹침
        );

        when(workSchedulesRepository.findByDate(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(existingSchedules);

        // When
        boolean result = scheduleValidator.isScheduleInsertable(newSchedule);

        // Then
        assertThat(result).isFalse();
    }

    private WorkSchedule createWorkSchedule(LocalTime startTime, LocalTime endTime) {
        LocalDate date = LocalDate.of(2023, 10, 1);
        return WorkSchedule.builder()
                .user(testUser)
                .startTime(LocalDateTime.of(date, startTime))
                .endTime(LocalDateTime.of(date, endTime))
                .statusCode(CodeType.WS02)
                .build();
    }

    private WorkSchedule createWorkScheduleWithDate(LocalDateTime startTime, LocalDateTime endTime) {
        return WorkSchedule.builder()
                .user(testUser)
                .startTime(startTime)
                .endTime(endTime)
                .statusCode(CodeType.WS02)
                .build();
    }

    @Test
    @DisplayName("엣지케이스 - MAX_CONCURRENT_SCHEDULES - 1개일 때 등록 가능")
    void isScheduleInsertable_OneBelowLimit_ReturnsTrue() {
        // Given
        WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 10, 0)
        );

        // 9:15 체크포인트에서 정확히 2개가 겹치도록 설정 (MAX=3, 2개는 허용)
        List<WorkSchedule> existingSchedules = List.of(
                createWorkSchedule(LocalTime.of(9, 0), LocalTime.of(9, 30)),   // 9:15에 겹침
                createWorkSchedule(LocalTime.of(9, 10), LocalTime.of(9, 45))  // 9:15에 겹침
        );

        when(workSchedulesRepository.findByDate(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(existingSchedules);

        // When
        boolean result = scheduleValidator.isScheduleInsertable(newSchedule);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("엣지케이스 - 정시가 아닌 시간의 스케줄")
    void isScheduleInsertable_NonStandardTime_ReturnsTrue() {
        // Given
        WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2023, 10, 1, 9, 7),
                LocalDateTime.of(2023, 10, 1, 10, 23)
        );

        List<WorkSchedule> existingSchedules = List.of(
                createWorkSchedule(LocalTime.of(9, 5), LocalTime.of(9, 35)),
                createWorkSchedule(LocalTime.of(9, 15), LocalTime.of(9, 45))
        );

        when(workSchedulesRepository.findByDate(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(existingSchedules);

        // When
        boolean result = scheduleValidator.isScheduleInsertable(newSchedule);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("엣지케이스 - 정시가 아닌 시간에서 최대 허용치 초과")
    void isScheduleInsertable_NonStandardTimeExceedsLimit_ReturnsFalse() {
        // Given
        WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2023, 10, 1, 9, 7),
                LocalDateTime.of(2023, 10, 1, 10, 23)
        );

        // 9:22 (첫 체크포인트)와 9:52 체크포인트에서 3개씩 겹치도록 설정
        List<WorkSchedule> existingSchedules = List.of(
                createWorkSchedule(LocalTime.of(9, 0), LocalTime.of(10, 0)),
                createWorkSchedule(LocalTime.of(9, 10), LocalTime.of(10, 10)),
                createWorkSchedule(LocalTime.of(9, 15), LocalTime.of(10, 15))
        );

        when(workSchedulesRepository.findByDate(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(existingSchedules);

        // When
        boolean result = scheduleValidator.isScheduleInsertable(newSchedule);

        // Then
        assertThat(result).isFalse();
    }


    @Test
    @DisplayName("엣지케이스 - 체크포인트 경계에서의 겹침 확인")
    void isScheduleInsertable_BoundaryCheckpoint() {
        // Given
        WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 9, 30)
        );

        // 9:15 체크포인트에서만 정확히 겹치는 스케줄들
        List<WorkSchedule> existingSchedules = List.of(
                createWorkSchedule(LocalTime.of(9, 0), LocalTime.of(9, 20)),   // 9:15에 겹침
                createWorkSchedule(LocalTime.of(9, 10), LocalTime.of(9, 25))  // 9:15에 겹침
        );

        when(workSchedulesRepository.findByDate(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(existingSchedules);

        // When
        boolean result = scheduleValidator.isScheduleInsertable(newSchedule);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("엣지케이스 - 여러 체크포인트 중 하나만 초과하는 경우")
    void isScheduleInsertable_OneCheckpointExceeds_ReturnsFalse() {
        // Given
        WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 11, 0)
        );

        // 9:45 체크포인트에서만 3개가 겹치도록 설정
        List<WorkSchedule> existingSchedules = List.of(
                createWorkSchedule(LocalTime.of(9, 30), LocalTime.of(10, 0)),   // 9:45에 겹침
                createWorkSchedule(LocalTime.of(9, 40), LocalTime.of(10, 10)),  // 9:45에 겹침
                createWorkSchedule(LocalTime.of(9, 35), LocalTime.of(9, 55))   // 9:45에 겹침
        );

        when(workSchedulesRepository.findByDate(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(existingSchedules);

        // When
        boolean result = scheduleValidator.isScheduleInsertable(newSchedule);

        // Then
        assertThat(result).isFalse();
    }

    // ========== 시간 검증 메서드 테스트 ==========

    @Test
    @DisplayName("최소 근무 시간 검증 - 2시간 이상일 때 통과")
    void validateMinWorkTime_Success() {
        // Given
        WorkScheduleCommand schedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 11, 0)  // 2시간 정확히
        );

        // When & Then - 예외가 발생하지 않아야 함
        scheduleValidator.validateMinWorkTime(schedule);
    }

    @Test
    @DisplayName("최소 근무 시간 검증 - 2시간 미만일 때 실패")
    void validateMinWorkTime_Failure() {
        // Given
        WorkScheduleCommand schedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2023, 10, 1, 9, 0),
                LocalDateTime.of(2023, 10, 1, 10, 30)  // 1.5시간
        );

        // When & Then
        assertThatThrownBy(() -> scheduleValidator.validateMinWorkTime(schedule))
                .isInstanceOf(BasicException.class);
    }

    @Test
    @DisplayName("최소 근무 시간 검증 - 경계값 테스트 (정확히 2시간)")
    void validateMinWorkTime_ExactlyTwoHours() {
        // Given
        WorkScheduleCommand schedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2023, 10, 1, 14, 0),
                LocalDateTime.of(2023, 10, 1, 16, 0)  // 정확히 2시간
        );

        // When & Then - 예외가 발생하지 않아야 함
        scheduleValidator.validateMinWorkTime(schedule);
    }

    @Test
    @DisplayName("최소 근무 시간 검증 - 경계값 테스트 (2시간 - 1분)")
    void validateMinWorkTime_OneLessThanTwoHours() {
        // Given
        WorkScheduleCommand schedule = new WorkScheduleCommand(
                1,
                LocalDateTime.of(2023, 10, 1, 14, 0),
                LocalDateTime.of(2023, 10, 1, 15, 59)  // 1시간 59분
        );

        // When & Then
        assertThatThrownBy(() -> scheduleValidator.validateMinWorkTime(schedule))
                .isInstanceOf(BasicException.class);
    }

    @Test
    @DisplayName("월 총 근무 시간 검증 - 27시간 이내일 때 통과")
    void validateTotalWorkTime_Success() {
        // Given
        long currentMonthlyMinutes = 24 * 60;  // 24시간 이미 근무
        long newSlotMinutes = 3 * 60;  // 3시간 추가 (총 27시간)

        // When & Then - 예외가 발생하지 않아야 함
        scheduleValidator.validateTotalWorkTime(currentMonthlyMinutes, newSlotMinutes);
    }

    @Test
    @DisplayName("월 총 근무 시간 검증 - 27시간 초과일 때 실패")
    void validateTotalWorkTime_Failure() {
        // Given
        long currentMonthlyMinutes = 25 * 60;  // 25시간 이미 근무
        long newSlotMinutes = 3 * 60;  // 3시간 추가 (총 28시간)

        // When & Then
        assertThatThrownBy(() -> scheduleValidator.validateTotalWorkTime(currentMonthlyMinutes, newSlotMinutes))
                .isInstanceOf(BasicException.class);
    }

    @Test
    @DisplayName("월 총 근무 시간 검증 - 경계값 테스트 (정확히 27시간)")
    void validateTotalWorkTime_ExactlyTwentySevenHours() {
        // Given
        long currentMonthlyMinutes = 20 * 60;  // 20시간 이미 근무
        long newSlotMinutes = 7 * 60;  // 7시간 추가 (정확히 27시간)

        // When & Then - 예외가 발생하지 않아야 함
        scheduleValidator.validateTotalWorkTime(currentMonthlyMinutes, newSlotMinutes);
    }

    @Test
    @DisplayName("월 총 근무 시간 검증 - 경계값 테스트 (27시간 + 1분)")
    void validateTotalWorkTime_OneMinuteOverTwentySevenHours() {
        // Given
        long currentMonthlyMinutes = 27 * 60;  // 27시간 이미 근무
        long newSlotMinutes = 1;  // 1분 추가

        // When & Then
        assertThatThrownBy(() -> scheduleValidator.validateTotalWorkTime(currentMonthlyMinutes, newSlotMinutes))
                .isInstanceOf(BasicException.class);
    }

    @Test
    @DisplayName("주 최대 근무 시간 검증 - 13시간 이내일 때 통과")
    void validateWeeklyWorkTime_Success() {
        // Given
        long currentWeeklyMinutes = 10 * 60;  // 10시간 이미 근무
        long newSlotMinutes = 3 * 60;  // 3시간 추가 (총 13시간)

        // When & Then - 예외가 발생하지 않아야 함
        scheduleValidator.validateWeeklyWorkTime(currentWeeklyMinutes, newSlotMinutes);
    }

    @Test
    @DisplayName("주 최대 근무 시간 검증 - 13시간 초과일 때 실패")
    void validateWeeklyWorkTime_Failure() {
        // Given
        long currentWeeklyMinutes = 11 * 60;  // 11시간 이미 근무
        long newSlotMinutes = 3 * 60;  // 3시간 추가 (총 14시간)

        // When & Then
        assertThatThrownBy(() -> scheduleValidator.validateWeeklyWorkTime(currentWeeklyMinutes, newSlotMinutes))
                .isInstanceOf(BasicException.class);
    }

    @Test
    @DisplayName("주 최대 근무 시간 검증 - 경계값 테스트 (정확히 13시간)")
    void validateWeeklyWorkTime_ExactlyThirteenHours() {
        // Given
        long currentWeeklyMinutes = 8 * 60;  // 8시간 이미 근무
        long newSlotMinutes = 5 * 60;  // 5시간 추가 (정확히 13시간)

        // When & Then - 예외가 발생하지 않아야 함
        scheduleValidator.validateWeeklyWorkTime(currentWeeklyMinutes, newSlotMinutes);
    }

    @Test
    @DisplayName("주 최대 근무 시간 검증 - 경계값 테스트 (13시간 + 1분)")
    void validateWeeklyWorkTime_OneMinuteOverThirteenHours() {
        // Given
        long currentWeeklyMinutes = 13 * 60;  // 13시간 이미 근무
        long newSlotMinutes = 1;  // 1분 추가

        // When & Then
        assertThatThrownBy(() -> scheduleValidator.validateWeeklyWorkTime(currentWeeklyMinutes, newSlotMinutes))
                .isInstanceOf(BasicException.class);
    }
}