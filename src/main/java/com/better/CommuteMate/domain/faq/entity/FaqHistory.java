package com.better.CommuteMate.domain.faq.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "faq_history")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FaqHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String title;  // 제목

    @Column(length = 100, nullable = false)
    private String category;  // 대분류명

    @Column(name = "sub_category", length = 100, nullable = false)
    private String subCategory;  // 소분류명

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;  // 내용

    @Column(name = "attachment_url", length = 150)
    private String attachmentUrl;  // 첨부파일 URL (nullable)

    @Column(length = 30, nullable = false)
    private String manager;  // 업무 담당자

    @Column(name = "writer_name", length = 30, nullable = false)
    private String writerName;  // 작성자 이름

    @Column(name = "editor_name", length = 30, nullable = false)
    private String editorName;  // 수정자 이름

    @Column(name = "edited_at", nullable = false)
    private LocalDateTime editedAt;  // 수정된 날짜

    // FK: faq_id → faq(id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faq_id", nullable = false)
    private Faq faq;

    @PrePersist
    protected void onCreate() {
        if (editedAt == null) {
            editedAt = LocalDateTime.now();
        }
    }
}