package com.better.CommuteMate.notification.application;

import com.better.CommuteMate.domain.notification.entity.Notification;
import com.better.CommuteMate.domain.notification.entity.NotificationCheckState;
import com.better.CommuteMate.domain.notification.repository.NotificationCheckStateRepository;
import com.better.CommuteMate.domain.notification.repository.NotificationRepository;
import com.better.CommuteMate.notification.controller.dtos.CheckNotificationResponse;
import com.better.CommuteMate.notification.controller.dtos.NewNotificationResponse;
import com.better.CommuteMate.notification.controller.dtos.NotificationListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationCheckStateRepository checkStateRepository;

    public NotificationListResponse getNotifications(Long userId) {
        List<Notification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId);

        LocalDateTime lastCheckedAt = resolveLastCheckedAt(userId);

        List<NotificationListResponse.NotificationItem> items = notifications.stream()
                .map(n -> toItem(n, lastCheckedAt))
                .toList();

        return new NotificationListResponse(items);
    }

    public NewNotificationResponse getNewNotificationStatus(Long userId) {
        LocalDateTime lastCheckedAt = resolveLastCheckedAt(userId);
        long count = lastCheckedAt == null
                ? notificationRepository.countByUserId(userId)
                : notificationRepository.countByUserIdAndCreatedAtAfter(userId, lastCheckedAt);
        return new NewNotificationResponse(count);
    }

    @Transactional
    public CheckNotificationResponse checkNotification(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        NotificationCheckState state = checkStateRepository.findById(userId)
                .orElse(null);

        if (state == null) {
            state = NotificationCheckState.builder()
                    .userId(userId)
                    .lastCheckedAt(now)
                    .build();
        } else {
            state.updateLastCheckedAt(now);
        }
        checkStateRepository.save(state);
        return new CheckNotificationResponse(now);
    }

    private LocalDateTime resolveLastCheckedAt(Long userId) {
        return checkStateRepository.findById(userId)
                .map(NotificationCheckState::getLastCheckedAt)
                .orElse(null);
    }

    private NotificationListResponse.NotificationItem toItem(
            Notification notification, LocalDateTime lastCheckedAt) {
        boolean isNew = lastCheckedAt == null
                || notification.getCreatedAt().isAfter(lastCheckedAt);

        return new NotificationListResponse.NotificationItem(
                notification.getNotificationId(),
                notification.getTypeCode().name(),
                notification.getTypeCode().getCodeValue(),
                notification.getTitle(),
                notification.getContent(),
                notification.getRefId(),
                notification.getCreatedAt(),
                isNew
        );
    }
}
