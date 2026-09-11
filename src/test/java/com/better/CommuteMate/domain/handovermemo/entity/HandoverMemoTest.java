package com.better.CommuteMate.domain.handovermemo.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class HandoverMemoTest {

    @Test
    void onCreateSetsCreatedAtToCurrentTime() {
        LocalDateTime before = LocalDateTime.now();
        HandoverMemo memo = new HandoverMemo();

        memo.onCreate();

        LocalDateTime after = LocalDateTime.now();
        assertThat(memo.getCreatedAt()).isBetween(before, after);
    }

    @Test
    void onCreateSetsExpiresAtThreeDaysAfterCreatedAt() {
        HandoverMemo memo = new HandoverMemo();

        memo.onCreate();

        assertThat(memo.getExpiresAt()).isEqualTo(memo.getCreatedAt().plusDays(3));
    }

    @Test
    void onCreateDoesNotOverwriteExplicitlySetCreatedAtOrExpiresAt() {
        LocalDateTime explicitCreatedAt = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime explicitExpiresAt = LocalDateTime.of(2026, 1, 10, 9, 0);
        HandoverMemo memo = HandoverMemo.builder()
                .createdAt(explicitCreatedAt)
                .expiresAt(explicitExpiresAt)
                .build();

        memo.onCreate();

        assertThat(memo.getCreatedAt()).isEqualTo(explicitCreatedAt);
        assertThat(memo.getExpiresAt()).isEqualTo(explicitExpiresAt);
    }
}
