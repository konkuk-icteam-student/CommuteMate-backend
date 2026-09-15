package com.better.CommuteMate.attendance.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NateOnCheckInNotifierTest {
    private final ExchangeFunction exchange = mock(ExchangeFunction.class);
    private final MockEnvironment environment = new MockEnvironment();
    private final NateOnCheckInNotifier notifier = new NateOnCheckInNotifier(
            WebClient.builder().exchangeFunction(exchange), environment);

    private CheckInCompleted event(Long organizationId) {
        return new CheckInCompleted(organizationId, "홍길동", LocalDateTime.now());
    }

    @Test
    @DisplayName("출근 알림 - 각 기관의 환경변수에 지정된 팀룸으로 한 번씩 전송")
    void routesToEachOrganizationsWebhook() {
        environment.setProperty("NATEON_WEBHOOK_ORG_17", "https://teamroom.nate.com/api/webhook/team-a");
        environment.setProperty("NATEON_WEBHOOK_ORG_42", "https://teamroom.nate.com/api/webhook/team-b");
        when(exchange.exchange(any())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK).build()));

        notifier.onCheckIn(event(17L));
        notifier.onCheckIn(event(42L));

        ArgumentCaptor<ClientRequest> requests = ArgumentCaptor.forClass(ClientRequest.class);
        verify(exchange, times(2)).exchange(requests.capture());
        assertThat(requests.getAllValues()).extracting(request -> request.url().toString())
                .containsExactly("https://teamroom.nate.com/api/webhook/team-a",
                        "https://teamroom.nate.com/api/webhook/team-b");
    }

    @Test
    @DisplayName("출근 알림 - 설정이 없거나 다른 기관만 설정되어 있으면 전송하지 않음")
    void skipsUnconfiguredOrganization() {
        notifier.onCheckIn(event(17L));
        environment.setProperty("NATEON_WEBHOOK_ORG_42", "https://teamroom.nate.com/api/webhook/team-b");
        notifier.onCheckIn(event(17L));
        verifyNoInteractions(exchange);
    }

    @Test
    @DisplayName("출근 알림 - 기관 ID가 없거나 양수가 아니면 전송하지 않음")
    void skipsInvalidOrganizationId() {
        environment.setProperty("NATEON_WEBHOOK_ORG_0", "https://teamroom.nate.com/api/webhook/test");
        notifier.onCheckIn(event(null));
        notifier.onCheckIn(event(0L));
        notifier.onCheckIn(event(-1L));
        verifyNoInteractions(exchange);
    }

    @Test
    @DisplayName("출근 알림 - 웹훅 주소가 공백이면 전송하지 않음")
    void skipsBlankWebhook() {
        environment.setProperty("NATEON_WEBHOOK_ORG_17", "   ");
        notifier.onCheckIn(event(17L));
        verifyNoInteractions(exchange);
    }

    @Test
    @DisplayName("출근 알림 - 허용되지 않은 웹훅 주소이면 전송하지 않음")
    void rejectsUntrustedWebhookDestination() {
        environment.setProperty("NATEON_WEBHOOK_ORG_17", "https://example.com/api/webhook/test");
        notifier.onCheckIn(event(17L));
        verifyNoInteractions(exchange);
    }

    @Test
    @DisplayName("출근 알림 - 전송 실패 시 출근 요청으로 예외를 전파하지 않음")
    void failureDoesNotEscapeToAttendanceRequest() {
        environment.setProperty("NATEON_WEBHOOK_ORG_17", "https://teamroom.nate.com/api/webhook/test");
        when(exchange.exchange(any())).thenReturn(Mono.error(new IllegalStateException("failed")));
        assertThatCode(() -> notifier.onCheckIn(event(17L))).doesNotThrowAnyException();
        verify(exchange).exchange(any());
    }
}
