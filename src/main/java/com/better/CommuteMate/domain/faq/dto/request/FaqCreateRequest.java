package com.better.CommuteMate.domain.faq.dto.request;

public record FaqCreateRequest(
        int userId, // Todo 인증 로직 추가되면 삭제
        String category,
        String subCategory,
        String title,
        String content,
        String attachmentUrl,
        String etc
) {}