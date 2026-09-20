package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.repository.WorkScheduleSettingRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkUnavailableTimeRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.notification.application.NotificationContentSerializer;
import com.better.CommuteMate.notification.application.NotificationService;
import com.better.CommuteMate.schedule.controller.admin.dtos.SaveScheduleSettingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyScheduleSettingServiceTest {

    @Mock WorkScheduleSettingRepository settingRepository;
    @Mock WorkSchedulesRepository scheduleRepository;
    @Mock WorkUnavailableTimeRepository unavailableTimeRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;
    @Mock NotificationContentSerializer notificationContentSerializer;

    MonthlyScheduleSettingService service;

    @BeforeEach
    void setUp() {
        service = new MonthlyScheduleSettingService(
                settingRepository, scheduleRepository, unavailableTimeRepository,
                userRepository, notificationService, notificationContentSerializer
        );
    }

    @Test
    @DisplayName("근로신청 설정 저장 - 변경된 규칙과 충돌하는 기존 스케줄을 취소한다")
    void saveCancelsSchedulesConflictingWithNewRule() {
        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .settingId(1L)
                .organizationId(10L)
                .year(2026)
                .month(4)
                .applyStartAt(LocalDateTime.of(2026, 4, 1, 0, 0))
                .applyEndAt(LocalDateTime.of(2026, 4, 10, 23, 59))
                .maxConcurrentWorkers(5)
                .minWorkUnitMinutes(30)
                .monthlyRequiredMinutes(1620)
                .build();
        User user = User.builder().userId(1L).build();
        WorkSchedule schedule = WorkSchedule.builder()
                .scheduleId(1L)
                .setting(setting)
                .user(user)
                .date(LocalDate.of(2026, 4, 19))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .statusCode(CodeType.WS02)
                .createdAt(LocalDateTime.of(2026, 4, 5, 12, 0))
                .build();
        SaveScheduleSettingRequest request = validRequest();

        when(settingRepository.findByOrganizationIdAndYearAndMonth(10L, 2026, 4))
                .thenReturn(Optional.of(setting));
        when(scheduleRepository.findAllBySettingAndStatusCodeIn(
                setting, List.of(CodeType.WS01, CodeType.WS02)
        )).thenReturn(List.of(schedule));

        var response = service.save(10L, 2026, 4, request, "99");

        assertThat(response.affectedScheduleCount).isEqualTo(1);
        assertThat(response.affectedUserCount).isEqualTo(1);
        assertThat(schedule.getStatusCode()).isEqualTo(CodeType.WS04);
        assertThat(setting.getMonthlyMaxMinutes()).isEqualTo(1620);
        assertThat(setting.getMonthlyRequiredMinutes()).isEqualTo(1620);
        verify(unavailableTimeRepository).deleteAllBySetting(setting);
        verify(unavailableTimeRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("근로신청 설정 저장 - 기존 설정 갱신 시 applyStartAt/applyEndAt이 시작일 00:00 / 종료일 23:59:59.999999999로 저장된다")
    void save_ExistingSetting_NormalizesApplyPeriod() {
        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .settingId(1L)
                .organizationId(10L)
                .year(2026)
                .month(4)
                .applyStartAt(LocalDateTime.of(2020, 1, 1, 0, 0))
                .applyEndAt(LocalDateTime.of(2020, 1, 2, 0, 0))
                .maxConcurrentWorkers(5)
                .minWorkUnitMinutes(30)
                .monthlyRequiredMinutes(1620)
                .build();
        SaveScheduleSettingRequest request = validRequest();

        when(settingRepository.findByOrganizationIdAndYearAndMonth(10L, 2026, 4))
                .thenReturn(Optional.of(setting));
        when(scheduleRepository.findAllBySettingAndStatusCodeIn(setting, List.of(CodeType.WS01, CodeType.WS02)))
                .thenReturn(List.of());

        service.save(10L, 2026, 4, request, "99");

        assertThat(setting.getApplyStartAt()).isEqualTo(LocalDate.of(2026, 4, 1).atStartOfDay());
        assertThat(setting.getApplyEndAt()).isEqualTo(LocalDate.of(2026, 4, 10).atTime(LocalTime.MAX));
        // 기존 설정 갱신이므로 NT03("근무 신청 시작")이 재발송되면 안 된다.
        verifyNoInteractions(userRepository, notificationService, notificationContentSerializer);
    }

    @Test
    @DisplayName("근로신청 설정 저장 - 신규 설정 생성 시 applyStartAt/applyEndAt이 시작일 00:00 / 종료일 23:59:59.999999999로 저장된다")
    void save_NewSetting_NormalizesApplyPeriod() {
        SaveScheduleSettingRequest request = validRequest();

        when(settingRepository.findByOrganizationIdAndYearAndMonth(10L, 2026, 4))
                .thenReturn(Optional.empty());
        when(settingRepository.save(any(WorkScheduleSetting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAllByOrganizationIdAndRoleCode(10L, CodeType.RL01))
                .thenReturn(List.of());

        service.save(10L, 2026, 4, request, "99");

        var captor = org.mockito.ArgumentCaptor.forClass(WorkScheduleSetting.class);
        verify(settingRepository).save(captor.capture());
        assertThat(captor.getValue().getApplyStartAt()).isEqualTo(LocalDate.of(2026, 4, 1).atStartOfDay());
        assertThat(captor.getValue().getApplyEndAt()).isEqualTo(LocalDate.of(2026, 4, 10).atTime(LocalTime.MAX));
    }

    @Test
    @DisplayName("근로신청 설정 저장 - 신규 오픈 시 조직 학생 전체에게 NT03 알림을 1회 생성한다")
    void save_NewSetting_NotifiesAllStudentsOnce() {
        SaveScheduleSettingRequest request = validRequest();
        User student1 = User.builder().userId(101L).organizationId(10L).roleCode(CodeType.RL01).build();
        User student2 = User.builder().userId(102L).organizationId(10L).roleCode(CodeType.RL01).build();

        when(settingRepository.findByOrganizationIdAndYearAndMonth(10L, 2026, 4))
                .thenReturn(Optional.empty());
        when(settingRepository.save(any(WorkScheduleSetting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAllByOrganizationIdAndRoleCode(10L, CodeType.RL01))
                .thenReturn(List.of(student1, student2));
        when(notificationContentSerializer.serialize(List.of())).thenReturn("[]");

        service.save(10L, 2026, 4, request, "99");

        // NT03은 변경 항목이 없으므로 content가 항상 빈 배열("[]")로 고정되어야 한다.
        verify(notificationService).notifyAll(
                eq(List.of(101L, 102L)), eq(CodeType.NT03),
                any(), eq("[]"), any()
        );
    }

    @Test
    @DisplayName("근로신청 설정 저장 - 조직에 학생이 없으면 NT03을 발송하지 않는다")
    void save_NewSetting_NoStudents_SkipsNotification() {
        SaveScheduleSettingRequest request = validRequest();

        when(settingRepository.findByOrganizationIdAndYearAndMonth(10L, 2026, 4))
                .thenReturn(Optional.empty());
        when(settingRepository.save(any(WorkScheduleSetting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAllByOrganizationIdAndRoleCode(10L, CodeType.RL01))
                .thenReturn(List.of());

        service.save(10L, 2026, 4, request, "99");

        verify(notificationService, never()).notifyAll(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("근로신청 설정 저장 - 최소 시간이 최대 시간보다 크면 실패한다")
    void saveRejectsMinimumGreaterThanMaximum() {
        SaveScheduleSettingRequest invalid = new SaveScheduleSettingRequest(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 10),
                List.of(),
                List.of(),
                4, 120, 600, 540, 1200, 1620
        );

        assertThatThrownBy(() -> service.save(10L, 2026, 4, invalid, "99"))
                .isInstanceOf(CustomException.class)
                .hasMessage("최소 근무시간은 최대 근무시간보다 작아야 합니다.");
    }

    private SaveScheduleSettingRequest validRequest() {
        return new SaveScheduleSettingRequest(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 10),
                List.of(LocalDate.of(2026, 4, 19)),
                List.of(new SaveScheduleSettingRequest.UnavailableTimeRange(
                        LocalTime.of(11, 0), LocalTime.of(13, 0)
                )),
                4, 120, 300, 540, 1200, 1620
        );
    }
}
