package com.better.CommuteMate.notification.application;

import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.repository.WorkScheduleSettingRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.code.CodeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkScheduleOpenNotificationSchedulerTest {

    @Mock WorkScheduleSettingRepository settingRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;
    @Mock NotificationContentSerializer notificationContentSerializer;

    @Test
    void notifyStartedApplications_SendsOnceAndMarksSetting() {
        WorkScheduleSetting setting = WorkScheduleSetting.builder()
                .settingId(5L).organizationId(10L).year(2026).month(11)
                .applyStartAt(LocalDateTime.of(2026, 10, 28, 19, 0))
                .applyEndAt(LocalDateTime.of(2026, 11, 3, 23, 59, 59))
                .maxConcurrentWorkers(4).monthlyRequiredMinutes(60)
                .build();
        User student = User.builder().userId(101L).organizationId(10L).roleCode(CodeType.RL01).build();
        LocalDateTime now = LocalDateTime.of(2026, 10, 28, 19, 0);

        when(settingRepository.findUnnotifiedStartedSettingsForUpdate(now)).thenReturn(List.of(setting));
        when(userRepository.findAllByOrganizationIdAndRoleCode(10L, CodeType.RL01)).thenReturn(List.of(student));
        when(notificationContentSerializer.serialize(List.of())).thenReturn("[]");

        scheduler().notifyStartedApplications(now);

        verify(notificationService).notifyAll(
                eq(List.of(101L)), eq(CodeType.NT03),
                eq("근무 신청이 시작되었습니다."), eq("[]"), eq("5")
        );
        assertThat(setting.getApplyStartNotifiedAt()).isEqualTo(now);
    }

    private WorkScheduleOpenNotificationScheduler scheduler() {
        return new WorkScheduleOpenNotificationScheduler(
                settingRepository, userRepository, notificationService, notificationContentSerializer
        );
    }
}
