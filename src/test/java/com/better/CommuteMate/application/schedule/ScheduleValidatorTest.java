package com.better.CommuteMate.application.schedule;

import com.better.CommuteMate.application.schedule.dtos.WorkScheduleCommand;
import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.repository.MonthlyScheduleLimitRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.code.CodeType;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleValidatorTest {

    @Mock
    private WorkSchedulesRepository workSchedulesRepository;

    @Mock
    private MonthlyScheduleLimitRepository monthlyScheduleLimitRepository;

    @InjectMocks
    private ScheduleValidator scheduleValidator;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduleValidator, "DEFAULT_MAX_CONCURRENT_SCHEDULES", 3);

        // 월별 제한이 없는 경우 기본값(3) 사용하도록 모킹
        when(monthlyScheduleLimitRepository.findByScheduleYearAndScheduleMonth(anyInt(), anyInt()))
                .thenReturn(Optional.empty());

        testUser = User.builder()
                .email("test@example.com")
                .name("Test User")
                .build();
    }

    @Test
    @DisplayName("해당 시간대에 겹치는 일정이 없어서 등록 가능한 경우")
    void isScheduleInsertable_NoOverlapping_ReturnsTrue() {
        // Given
        WorkScheduleCommand newSchedule = new WorkScheduleCommand(
                "test@example.com",
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
                "test@example.com",
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
                "test@example.com",
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
                "test@example.com",
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
                "test@example.com",
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
                "test@example.com",
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
                "test@example.com",
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
                "test@example.com",
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
                "test@example.com",
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
                "test@example.com",
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
                "test@example.com",
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
                "test@example.com",
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
}