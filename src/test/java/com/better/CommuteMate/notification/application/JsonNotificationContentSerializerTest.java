package com.better.CommuteMate.notification.application;

import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.notification.application.dtos.NotificationChangeItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 스프링이 자동 구성하는 ObjectMapper(빈)를 그대로 사용해 검증한다.
 * 순수 `new ObjectMapper()`는 Spring의 Jackson2ObjectMapperBuilder가 적용하는
 * WRITE_DATES_AS_TIMESTAMPS=false 커스터마이징이 없어 프로덕션과 직렬화 결과가 달라진다.
 */
@JsonTest
class JsonNotificationContentSerializerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void serialize_writesDateTimeDurationAndChangeTypeAsJsonArray() {
        JsonNotificationContentSerializer serializer = new JsonNotificationContentSerializer(objectMapper);
        NotificationChangeItem item = new NotificationChangeItem(
                LocalDate.of(2026, 4, 6), LocalTime.of(13, 0), LocalTime.of(13, 30),
                30L, CodeType.CR01
        );

        String content = serializer.serialize(List.of(item));

        assertThat(content).contains("\"2026-04-06\"");
        assertThat(content).contains("\"13:00");
        assertThat(content).contains("\"13:30");
        assertThat(content).contains("\"durationMinutes\":30");
        assertThat(content).contains("\"changeTypeCode\":\"CR01\"");
    }

    @Test
    void serialize_emptyList_producesEmptyJsonArray() {
        JsonNotificationContentSerializer serializer = new JsonNotificationContentSerializer(objectMapper);
        assertThat(serializer.serialize(List.of())).isEqualTo("[]");
    }
}
