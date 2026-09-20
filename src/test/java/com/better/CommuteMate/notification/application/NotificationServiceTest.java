package com.better.CommuteMate.notification.application;

import com.better.CommuteMate.domain.notification.entity.Notification;
import com.better.CommuteMate.domain.notification.entity.NotificationCheckState;
import com.better.CommuteMate.domain.notification.repository.NotificationCheckStateRepository;
import com.better.CommuteMate.domain.notification.repository.NotificationRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.notification.controller.dtos.NotificationListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock NotificationCheckStateRepository checkStateRepository;
    @Mock NotificationContentSerializer notificationContentSerializer;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(
                notificationRepository, checkStateRepository, notificationContentSerializer
        );
    }

    @Test
    void getNotifications_returnsCreatedAtWithoutOffset() {
        LocalDateTime storedCreatedAt = LocalDateTime.of(2026, 3, 20, 16, 21);
        Notification notification = Notification.builder()
                .notificationId(1L)
                .userId(7L)
                .typeCode(CodeType.NT02)
                .title("근무 시간 수정이 거절되었습니다.")
                .createdAt(storedCreatedAt)
                .build();
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(notification));
        when(checkStateRepository.findById(7L)).thenReturn(Optional.empty());

        NotificationListResponse response = service.getNotifications(7L);

        assertThat(response.notifications).singleElement()
                .extracting(NotificationListResponse.NotificationItem::createdAt)
                .isEqualTo(storedCreatedAt);
    }

    @Test
    void getNotifications_isNewJudgementUsesRawStoredCreatedAtNotDisplayCorrectedValue() {
        // lastCheckedAt과 createdAt은 둘 다 저장값(UTC) 기준으로 비교되어야 하며,
        // 출력 보정(+9h)이 이 판정에 영향을 주면 안 된다.
        LocalDateTime lastCheckedAt = LocalDateTime.of(2026, 3, 20, 12, 0);
        Notification beforeCheck = Notification.builder()
                .notificationId(1L).userId(7L).typeCode(CodeType.NT01)
                .title("이전 알림").createdAt(lastCheckedAt.minusMinutes(1)).build();
        Notification afterCheck = Notification.builder()
                .notificationId(2L).userId(7L).typeCode(CodeType.NT02)
                .title("새 알림").createdAt(lastCheckedAt.plusMinutes(1)).build();
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(afterCheck, beforeCheck));
        when(checkStateRepository.findById(7L)).thenReturn(Optional.of(
                NotificationCheckState.builder().userId(7L).lastCheckedAt(lastCheckedAt).build()
        ));

        NotificationListResponse response = service.getNotifications(7L);

        assertThat(response.notifications).hasSize(2);
        assertThat(response.notifications.get(0).isNew()).isTrue();
        assertThat(response.notifications.get(1).isNew()).isFalse();
    }

    @Test
    void getNotifications_mapsContentViaSerializerAndExposesRejectReason() {
        Notification rejected = Notification.builder()
                .notificationId(1L).userId(7L).typeCode(CodeType.NT02)
                .title("근무 시간 수정이 거절되었습니다.")
                .content("[{\"date\":\"2026-04-06\"}]")
                .rejectReason("정원 초과로 반려합니다.")
                .createdAt(LocalDateTime.of(2026, 3, 20, 16, 21))
                .build();
        List<NotificationListResponse.ContentItem> parsedContent = List.of(
                new NotificationListResponse.ContentItem(
                        java.time.LocalDate.of(2026, 4, 6),
                        java.time.LocalTime.of(13, 0), java.time.LocalTime.of(13, 30),
                        30, "CR01"
                )
        );
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(rejected));
        when(checkStateRepository.findById(7L)).thenReturn(Optional.empty());
        when(notificationContentSerializer.parse("[{\"date\":\"2026-04-06\"}]"))
                .thenReturn(parsedContent);

        NotificationListResponse response = service.getNotifications(7L);

        assertThat(response.notifications).singleElement().satisfies(item -> {
            assertThat(item.content()).isEqualTo(parsedContent);
            assertThat(item.rejectReason()).isEqualTo("정원 초과로 반려합니다.");
        });
    }

    @Test
    void notify_savesSingleNotificationWithGivenFields() {
        service.notify(7L, CodeType.NT01, "근무 시간 수정이 승인되었습니다.", "[]", "42", null);

        var captor = org.mockito.ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getTypeCode()).isEqualTo(CodeType.NT01);
        assertThat(saved.getTitle()).isEqualTo("근무 시간 수정이 승인되었습니다.");
        assertThat(saved.getContent()).isEqualTo("[]");
        assertThat(saved.getRefId()).isEqualTo("42");
        assertThat(saved.getRejectReason()).isNull();
    }

    @Test
    void notify_withRejectReason_savesItOnNotification() {
        service.notify(7L, CodeType.NT02, "근무 시간 수정이 거절되었습니다.", "[]", "42", "정원 초과");

        var captor = org.mockito.ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getRejectReason()).isEqualTo("정원 초과");
    }

    @Test
    void notifyAll_savesOneNotificationPerUserIdViaSaveAll() {
        service.notifyAll(
                List.of(1L, 2L, 3L), CodeType.NT03,
                "근무 신청이 시작되었습니다.", "2026년 4월 신청 기간", null
        );

        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        @SuppressWarnings("unchecked")
        List<Notification> saved = captor.getValue();
        assertThat(saved).hasSize(3);
        assertThat(saved).extracting(Notification::getUserId).containsExactly(1L, 2L, 3L);
        assertThat(saved).allMatch(n -> n.getTypeCode() == CodeType.NT03);
    }
}
