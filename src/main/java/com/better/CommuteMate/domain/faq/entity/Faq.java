package com.better.CommuteMate.domain.faq.entity;

import com.better.CommuteMate.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "faq")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "category", length = 100, nullable = false)
    private String category;

    @Column(name = "sub_category", length = 100, nullable = false)
    private String subCategory;

    @Column(name = "title", length = 100, nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "etc", columnDefinition = "TEXT")
    private String etc;

    @Column(name = "attachment_url", length = 150)
    private String attachmentUrl;

    @Column(name = "writer_name", length = 30, nullable = false)
    private String writerName;

    @Column(name = "last_edited_at", nullable = false)
    private LocalDateTime lastEditedAt;

    @Column(name = "last_editor_name", length = 30, nullable = false)
    private String lastEditorName;

    @Column(name = "manager", length = 30, nullable = false)
    private String manager;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @Column(name = "deleted_flag", nullable = false)
    private Boolean deletedFlag = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id")
    private User writer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_editor_id")
    private User lastEditor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id")
    private SubCategory subCategoryEntity;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastEditedAt = LocalDateTime.now();
    }
}
