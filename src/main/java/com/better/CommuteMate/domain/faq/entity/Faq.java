package com.better.CommuteMate.domain.faq.entity;

import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.domain.manager.entity.Manager;
import com.better.CommuteMate.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "faq")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(name = "complainant_name", length = 30)
    private String complainantName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;

    @Column(columnDefinition = "TEXT")
    private String etc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id", nullable = false)
    private User writer;

    @Column(name = "last_edited_at", nullable = false)
    private LocalDateTime lastEditedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_flag", nullable = false)
    private Boolean deletedFlag;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastEditedAt = LocalDateTime.now();
        this.deletedFlag = false;
    }

    @PreUpdate
    void onUpdate() {
        this.lastEditedAt = LocalDateTime.now();
    }

    @Builder
    private Faq(
            String title,
            String complainantName,
            String content,
            String answer,
            String etc,
            User writer,
            Category category
    ) {
        this.title = title;
        this.complainantName = complainantName;
        this.content = content;
        this.answer = answer;
        this.etc = etc;
        this.writer = writer;
        this.category = category;
    }

    public static Faq create(
            String title,
            String complainantName,
            String content,
            String answer,
            String etc,
            User writer,
            Category category
    ) {
        return Faq.builder()
                .title(title)
                .complainantName(complainantName)
                .content(content)
                .answer(answer)
                .etc(etc)
                .writer(writer)
                .category(category)
                .build();
    }

}