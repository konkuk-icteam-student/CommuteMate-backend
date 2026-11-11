package com.better.CommuteMate.attendance.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class NateOnNotificationNotifier {
    private static final Duration WEBHOOK_TIMEOUT = Duration.ofSeconds(15);

    private final WebClient client;
    private final Environment environment;

    public NateOnNotificationNotifier(WebClient.Builder builder, Environment environment) {
        this.client = builder.build();
        this.environment = environment;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCheckIn(CheckInCompleted event) {
        String content = "[출근 알림] " + event.studentName() + " 학생이 "
                + event.checkTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                + "에 출근했습니다.";
        send(event.organizationId(), "check-in", content);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWorkScheduleChangeRequested(WorkScheduleChangeRequested event) {
        String content = "[일정 수정 요청] " + event.studentName()
                + " 학생이 일정 수정 요청을 등록했습니다.\n관리자 페이지에서 확인해주세요.";
        send(event.organizationId(), "schedule-change-request", content);
    }

    private void send(Long organizationId, String notificationType, String content) {
        if (organizationId == null || organizationId <= 0) {
            log.warn("NateOn notification skipped: type={}, invalidOrganizationId={}",
                    notificationType, organizationId);
            return;
        }

        try {
            String webhookUrl = environment.getProperty("NATEON_WEBHOOK_ORG_" + organizationId, "").trim();
            if (webhookUrl.isBlank()) {
                log.warn("NateOn webhook is not configured: type={}, organizationId={}",
                        notificationType, organizationId);
                return;
            }

            URI uri = URI.create(webhookUrl);
            if (!"https".equals(uri.getScheme()) || !"teamroom.nate.com".equals(uri.getHost())
                    || !uri.getPath().startsWith("/api/webhook/") || uri.getUserInfo() != null) {
                log.warn("NateOn webhook configuration is invalid: type={}, organizationId={}",
                        notificationType, organizationId);
                return;
            }

            long startedAt = System.nanoTime();
            client.post().uri(uri)
                    .body(BodyInserters.fromFormData("content", content))
                    .retrieve().toBodilessEntity()
                    .timeout(WEBHOOK_TIMEOUT)
                    .subscribe(
                            response -> log.info(
                                    "NateOn notification sent: type={}, organizationId={}, status={}, elapsedMs={}",
                                    notificationType,
                                    organizationId,
                                    response.getStatusCode().value(),
                                    elapsedMillis(startedAt)
                            ),
                            error -> log.warn(
                                    "NateOn notification failed: type={}, organizationId={}, elapsedMs={}, errorType={}, message={}",
                                    notificationType,
                                    organizationId,
                                    elapsedMillis(startedAt),
                                    error.getClass().getSimpleName(),
                                    error.getMessage()
                            )
                    );
        } catch (RuntimeException error) {
            log.warn("NateOn notification could not be sent: type={}, organizationId={}, errorType={}, message={}",
                    notificationType, organizationId, error.getClass().getSimpleName(), error.getMessage());
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
