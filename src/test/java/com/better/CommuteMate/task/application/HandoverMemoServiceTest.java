package com.better.CommuteMate.task.application;

import com.better.CommuteMate.domain.handovermemo.entity.HandoverMemo;
import com.better.CommuteMate.domain.handovermemo.repository.HandoverMemoRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandoverMemoServiceTest {

    @Mock HandoverMemoRepository handoverMemoRepository;

    private HandoverMemoService service;

    @BeforeEach
    void setUp() {
        service = new HandoverMemoService(handoverMemoRepository);
    }

    @Test
    void returnsOrganizationMemosForRequestedDate() {
        User creator = User.builder().userId(7L).name("홍길동").build();
        HandoverMemo memo = HandoverMemo.builder()
                .memoId(1L)
                .organizationId(10L)
                .content("다음 근무자가 쓰레기봉투 꼭 갈아주세요.")
                .createdBy(creator)
                .createdAt(LocalDateTime.of(2026, 4, 15, 10, 36))
                .build();
        when(handoverMemoRepository.findDailyMemos(
                10L,
                LocalDateTime.of(2026, 4, 15, 0, 0),
                LocalDateTime.of(2026, 4, 16, 0, 0)
        )).thenReturn(List.of(memo));

        var response = service.getMemos(10L, "2026-04-15", 7L);

        assertThat(response.memoCount).isEqualTo(1);
        assertThat(response.memos).singleElement().satisfies(item -> {
            assertThat(item.memoId()).isEqualTo(1L);
            assertThat(item.isMine()).isTrue();
            assertThat(item.createdBy().userId()).isEqualTo(7L);
            assertThat(item.createdBy().name()).isEqualTo("홍길동");
        });
    }

    @Test
    void marksMemosNotWrittenByCurrentUserAsNotMine() {
        User creator = User.builder().userId(7L).name("홍길동").build();
        HandoverMemo memo = HandoverMemo.builder()
                .memoId(1L)
                .organizationId(10L)
                .content("다음 근무자가 쓰레기봉투 꼭 갈아주세요.")
                .createdBy(creator)
                .createdAt(LocalDateTime.of(2026, 4, 15, 10, 36))
                .build();
        when(handoverMemoRepository.findDailyMemos(
                10L,
                LocalDateTime.of(2026, 4, 15, 0, 0),
                LocalDateTime.of(2026, 4, 16, 0, 0)
        )).thenReturn(List.of(memo));

        var response = service.getMemos(10L, "2026-04-15", 99L);

        assertThat(response.memos).singleElement()
                .satisfies(item -> assertThat(item.isMine()).isFalse());
    }

    @Test
    void rejectsInvalidDateFormat() {
        assertThatThrownBy(() -> service.getMemos(10L, "2026/04/15", 7L))
                .isInstanceOf(CustomException.class)
                .hasMessage("날짜 형식이 올바르지 않습니다.");
    }
}
