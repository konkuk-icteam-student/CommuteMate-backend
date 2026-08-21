package com.better.CommuteMate.domain.handovermemo.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

class HandoverMemoTest {

    private TimeZone originalDefaultTimeZone;

    @BeforeEach
    void fixDefaultTimeZoneToUtc() {
        originalDefaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void restoreDefaultTimeZone() {
        TimeZone.setDefault(originalDefaultTimeZone);
    }

    @Test
    void onCreateStoresCreatedAtInKstEvenWhenJvmDefaultIsUtc() {
        Instant before = Instant.now();
        HandoverMemo memo = new HandoverMemo();

        memo.onCreate(Clock.system(ZoneId.of("Asia/Seoul")));

        // JVM 기본 타임존이 UTC인 상황에서도 createdAt이 Asia/Seoul 기준임을 확인.
        // 같은 순간을 UTC 벽시계로 표현한 값과 비교해 9시간 차이가 나야 함(약간의
        // 실행 지연을 허용하기 위해 2초 오차 허용).
        LocalDateTime naiveUtcAtSameInstant = LocalDateTime.ofInstant(before, ZoneId.of("UTC"));
        assertThat(Duration.between(naiveUtcAtSameInstant, memo.getCreatedAt()))
                .isCloseTo(Duration.ofHours(9), Duration.ofSeconds(2));
    }

    @Test
    void onCreateKeepsExpiresAtThreeDaysAfterCreatedAtOnSameKstBasis() {
        HandoverMemo memo = new HandoverMemo();

        memo.onCreate(Clock.system(ZoneId.of("Asia/Seoul")));

        assertThat(memo.getExpiresAt()).isEqualTo(memo.getCreatedAt().plusDays(3));
    }

    @Test
    void onCreateDoesNotShiftDateBackForKstEarlyMorningMoment() {
        Instant kstMidnightThirty = LocalDateTime.of(2026, 8, 22, 0, 30)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toInstant();
        Clock fixedKstClock = Clock.fixed(kstMidnightThirty, ZoneId.of("Asia/Seoul"));
        HandoverMemo memo = new HandoverMemo();

        memo.onCreate(fixedKstClock);

        assertThat(memo.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 22, 0, 30));
        assertThat(memo.getCreatedAt().toLocalDate()).isEqualTo(LocalDate.of(2026, 8, 22));
    }

    @Test
    void onCreateDoesNotOverwriteExplicitlySetCreatedAtOrExpiresAt() {
        LocalDateTime explicitCreatedAt = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime explicitExpiresAt = LocalDateTime.of(2026, 1, 10, 9, 0);
        HandoverMemo memo = HandoverMemo.builder()
                .createdAt(explicitCreatedAt)
                .expiresAt(explicitExpiresAt)
                .build();

        memo.onCreate(Clock.system(ZoneId.of("Asia/Seoul")));

        assertThat(memo.getCreatedAt()).isEqualTo(explicitCreatedAt);
        assertThat(memo.getExpiresAt()).isEqualTo(explicitExpiresAt);
    }
}
