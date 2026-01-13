package com.better.CommuteMate.domain.faq.entity;

import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "faq")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // category 엔티티와 FK 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "TEXT")
    private String etc;

    @Column(name = "attachment_url", length = 150)
    private String attachmentUrl;

    @Column(name = "writer_name", length = 30, nullable = false)
    private String writerName;

    @Column(name = "last_edited_at", nullable = false)
    private LocalDateTime lastEditedAt;

    @Column(name = "last_editor_name", length = 30, nullable = false)
    private String lastEditorName;

    @Column(length = 30, nullable = false)
    private String manager;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_flag", nullable = false)
    private Boolean deletedFlag;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id", nullable = false)
    private User writer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_editor_id", nullable = false)
    private User lastEditor;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastEditedAt = LocalDateTime.now();
    }
}