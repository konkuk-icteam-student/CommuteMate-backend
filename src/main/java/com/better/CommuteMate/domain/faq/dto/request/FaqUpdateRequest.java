package com.better.CommuteMate.domain.faq.dto.request;

public record FaqUpdateRequest(
        int userId,              // 수정자 userId (임시)
        String title,            // 제목 (필수)
        String content,          // 내용 (필수)
        String etc,              // 비고 (필수)
        String attachmentUrl,    // 첨부파일 URL (선택)
        String manager,          // 담당자 이름 (필수)
        String editorName        // 수정자 이름 (필수)
) {}