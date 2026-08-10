package com.better.CommuteMate.task.controller.dtos;

import com.better.CommuteMate.domain.handovermemo.entity.HandoverMemo;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;

import java.time.LocalDateTime;

public class CreateHandoverMemoResponse extends ResponseDetail {
    public final Long memoId;
    public final String content;
    public final CreatedBy createdBy;
    public final LocalDateTime createdAt;
    public final LocalDateTime expiresAt;

    private CreateHandoverMemoResponse(HandoverMemo memo) {
        this.memoId = memo.getMemoId();
        this.content = memo.getContent();
        this.createdBy = new CreatedBy(
                memo.getCreatedBy().getUserId(),
                memo.getCreatedBy().getName()
        );
        this.createdAt = memo.getCreatedAt();
        this.expiresAt = memo.getExpiresAt();
    }

    public static CreateHandoverMemoResponse from(HandoverMemo memo) {
        return new CreateHandoverMemoResponse(memo);
    }

    public record CreatedBy(Long userId, String name) {
    }
}
