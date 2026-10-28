package com.better.CommuteMate.notification.application;

import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.repository.WorkScheduleSettingRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.code.CodeType;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 신청 시작 시각이 지난 월별 설정에 NT03 알림을 한 번만 발송한다. */
@Service
@RequiredArgsConstructor
public class WorkScheduleOpenNotificationScheduler {

    private final WorkScheduleSettingRepository settingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final NotificationContentSerializer notificationContentSerializer;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    @Transactional
    public void notifyStartedApplications() {
        notifyStartedApplications(LocalDateTime.now());
    }

    void notifyStartedApplications(LocalDateTime now) {
        List<WorkScheduleSetting> settings = settingRepository
                .findUnnotifiedStartedSettingsForUpdate(now);

        for (WorkScheduleSetting setting : settings) {
            List<Long> studentIds = userRepository
                    .findAllByOrganizationIdAndRoleCode(setting.getOrganizationId(), CodeType.RL01)
                    .stream()
                    .map(User::getUserId)
                    .toList();

            notificationService.notifyAll(
                    studentIds,
                    CodeType.NT03,
                    "근무 신청이 시작되었습니다.",
                    notificationContentSerializer.serialize(List.of()),
                    String.valueOf(setting.getSettingId())
            );
            setting.markApplyStartNotified(now);
        }
    }
}
