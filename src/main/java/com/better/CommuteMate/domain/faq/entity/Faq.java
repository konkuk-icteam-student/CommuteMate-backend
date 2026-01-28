package com.better.CommuteMate.domain.faq.entity;

import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "updated_date", nullable = false)
    private LocalDate updatedDate;

    @Column(name = "deleted_flag", nullable = false)
    private Boolean deletedFlag;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.updatedDate = LocalDate.now();
        this.deletedFlag = false;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedDate = LocalDate.now();
    }

    public static Faq create(
            String title,
            String complainantName,
            String content,
            String answer,
            String etc,
            Category category,
            User writer
    ) {
        Faq faq = new Faq();
        faq.title = title;
        faq.complainantName = complainantName;
        faq.content = content;
        faq.answer = answer;
        faq.etc = etc;
        faq.category = category;
        faq.writer = writer;
        return faq;
    }

    public void update(
            String title,
            String complainantName,
            String content,
            String answer,
            String etc,
            Category category,
            User writer
    ) {
        this.title = title;
        this.complainantName = complainantName;
        this.content = content;
        this.answer = answer;
        this.etc = etc;
        this.category = category;
        this.writer = writer;
    }
}