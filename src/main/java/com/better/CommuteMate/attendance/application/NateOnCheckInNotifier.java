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
public class NateOnCheckInNotifier {
    private final WebClient client;
    private final Environment environment;

    public NateOnCheckInNotifier(WebClient.Builder builder, Environment environment) {
        this.client = builder.build();
        this.environment = environment;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCheckIn(CheckInCompleted event) {
        if (event.organizationId() == null || event.organizationId() <= 0) {
            return;
        }
        try {
            String webhookUrl = environment.getProperty("NATEON_WEBHOOK_ORG_" + event.organizationId(), "").trim();
            if (webhookUrl.isBlank()) {
                return;
            }
            URI uri = URI.create(webhookUrl);
            if (!"https".equals(uri.getScheme()) || !"teamroom.nate.com".equals(uri.getHost())
                    || !uri.getPath().startsWith("/api/webhook/") || uri.getUserInfo() != null) {
                log.warn("NateOn webhook configuration is invalid; notification skipped");
                return;
            }
            String content = "[출근 알림] " + event.studentName() + " 학생이 "
                    + event.checkTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    + "에 출근했습니다.";
            client.post().uri(uri)
                    .body(BodyInserters.fromFormData("content", content))
                    .retrieve().toBodilessEntity()
                    .timeout(Duration.ofSeconds(5))
                    .subscribe(response -> { }, error -> log.warn(
                            "NateOn notification failed ({})", error.getClass().getSimpleName()));
        } catch (RuntimeException error) {
            log.warn("NateOn notification could not be sent ({})", error.getClass().getSimpleName());
        }
    }
}
