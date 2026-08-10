package com.better.CommuteMate.task.controller.dtos;

import com.better.CommuteMate.domain.handovermemo.entity.HandoverMemo;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class HandoverMemosResponse extends ResponseDetail {
    public final LocalDate date;
    public final int memoCount;
    public final List<MemoItem> memos;

    public HandoverMemosResponse(LocalDate date, List<MemoItem> memos) {
        this.date = date;
        this.memoCount = memos.size();
        this.memos = memos;
    }

    public static MemoItem toItem(HandoverMemo memo) {
        return new MemoItem(
                memo.getMemoId(),
                memo.getContent(),
                new CreatedBy(
                        memo.getCreatedBy().getUserId(),
                        memo.getCreatedBy().getName()
                ),
                memo.getCreatedAt()
        );
    }

    @Override
    @JsonIgnore
    public LocalDateTime getTimestamp() {
        return super.getTimestamp();
    }

    public record MemoItem(
            Long memoId,
            String content,
            CreatedBy createdBy,
            LocalDateTime createdAt
    ) {
    }

    public record CreatedBy(Long userId, String name) {
    }
}
