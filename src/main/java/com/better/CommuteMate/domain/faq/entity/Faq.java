package com.better.CommuteMate.domain.faq.entity;

import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.FaqErrorCode;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "faq")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30, nullable = false)
    private String title;

    @Column(name = "complainant_name", length = 50)
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

    @OneToMany(mappedBy = "faq", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FaqCategory> faqCategories = new ArrayList<>();

    @Column(name = "updated_date", nullable = false)
    private LocalDate updatedDate;

    @Column(name = "deleted_flag", nullable = false)
    private Boolean deletedFlag;

    @Column(name = "deleted_at")
    private LocalDate deletedAt;

    public void addCategory(Category category) {
        if (this.faqCategories.size() >= 3) {
            throw CustomException.of(FaqErrorCode.CATEGORY_LIMIT_EXCEEDED);
        }

        FaqCategory fc = new FaqCategory(this, category);
        this.faqCategories.add(fc);
    }

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
            List<Category> categories,
            User writer
    ) {
        Faq faq = new Faq();
        faq.title = title;
        faq.complainantName = complainantName;
        faq.content = content;
        faq.answer = answer;
        faq.etc = etc;
        faq.writer = writer;

        for (Category category : categories) {
            faq.addCategory(category);
        }

        return faq;
    }

    public void update(
            String title,
            String complainantName,
            String content,
            String answer,
            String etc,
            List<Category> categories,
            User writer
    ) {
        this.title = title;
        this.complainantName = complainantName;
        this.content = content;
        this.answer = answer;
        this.etc = etc;
        this.writer = writer;

        this.faqCategories.clear();

        for (Category category : categories) {
            this.addCategory(category);
        }
    }

    public void delete() {
        this.deletedFlag = true;
        this.deletedAt = LocalDate.now();
    }

}