package com.better.CommuteMate.notification.application;

import com.better.CommuteMate.notification.application.dtos.NotificationChangeItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 알림 content를 JSON 배열 문자열로 직렬화한다.
 * 프론트 응답 포맷이 확정되면 이 구현체만 교체하면 되도록
 * 항목 추출(호출부)과 직렬화(여기)를 분리해둔다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonNotificationContentSerializer implements NotificationContentSerializer {

    private final ObjectMapper objectMapper;

    @Override
    public String serialize(List<NotificationChangeItem> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            log.error("알림 변경 항목 직렬화 실패, 빈 배열로 대체합니다.", e);
            return "[]";
        }
    }
}
