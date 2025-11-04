package com.better.CommuteMate.domain.faq.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "faq_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaqHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "title", length = 100, nullable = false)
    private String title;

    @Column(name = "category", length = 100, nullable = false)
    private String category;

    @Column(name = "sub_category", length = 100, nullable = false)
    private String subCategory;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "attachment_url", length = 150)
    private String attachmentUrl;

    @Column(name = "manager", length = 30, nullable = false)
    private String manager;

    @Column(name = "writer_name", length = 30, nullable = false)
    private String writerName;

    @Column(name = "editor_name", length = 30, nullable = false)
    private String editorName;

    @Column(name = "edited_at", nullable = false)
    private LocalDateTime editedAt;

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
