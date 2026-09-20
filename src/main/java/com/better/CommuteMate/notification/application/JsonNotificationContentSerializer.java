package com.better.CommuteMate.notification.application;

import com.better.CommuteMate.notification.application.dtos.NotificationChangeItem;
import com.better.CommuteMate.notification.controller.dtos.NotificationListResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 알림 content를 저장(JSON 문자열)/응답(구조화된 배열) 양방향으로 변환한다.
 * 프론트 응답 포맷이 바뀌어도 이 구현체만 교체하면 되도록
 * 항목 추출(호출부)과 직렬화/역직렬화(여기)를 분리해둔다.
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

    @Override
    public List<NotificationListResponse.ContentItem> parse(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(
                    content, new TypeReference<List<NotificationListResponse.ContentItem>>() {}
            );
        } catch (JsonProcessingException e) {
            log.error("알림 content 파싱 실패, 빈 배열로 대체합니다.", e);
            return List.of();
        }
    }
}
