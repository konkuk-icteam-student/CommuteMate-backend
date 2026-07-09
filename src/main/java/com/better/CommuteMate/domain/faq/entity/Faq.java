package com.better.CommuteMate.domain.faq.entity;

import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.FaqErrorCode;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Table(name = "faq")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30)
    private String title; // Todo nullable=true로 수정 완료

    @Column(name = "complainant_name", length = 50)
    private String complainantName;

    @Column(columnDefinition = "TEXT")
    private String content; // Todo nullable=true로 수정 완료

    @Column(columnDefinition = "TEXT")
    private String answer; // Todo nullable=true로 수정 완료

    @Column(columnDefinition = "TEXT")
    private String etc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id")
    private User writer; // Todo nullable=true로 수정 완료

    @OneToMany(mappedBy = "faq", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FaqCategory> faqCategories = new ArrayList<>();

    @Column(name = "updated_date", nullable = false)
    private LocalDate updatedDate;

    @Column(name = "deleted_flag", nullable = false)
    private Boolean deletedFlag;

    @Column(name = "deleted_at")
    private LocalDate deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FaqStatus status;

    @OneToMany(mappedBy = "faq", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FaqImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "faq", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FaqFile> files = new ArrayList<>();

    @OneToMany(mappedBy = "faq", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FaqRelation> relatedFaqRelations = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(columnDefinition = "vector(1536)")
    private float[] embedding;

    public void updateEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public void updateRelatedFaqRelations(List<Faq> faqs) {
        this.relatedFaqRelations.clear();
        for (Faq related : faqs) {
            this.relatedFaqRelations.add(new FaqRelation(this, related));
        }
    }

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
            User writer,
            FaqStatus status
    ) {
        Faq faq = new Faq();
        faq.title = title;
        faq.complainantName = complainantName;
        faq.content = content;
        faq.answer = answer;
        faq.etc = etc;
        faq.writer = writer;
        faq.status = status;

        for (Category category : categories) {
            faq.addCategory(category);
        }

        return faq;
    }

    public void update( // Todo FaqStatus 추가하기
            String title,
            String complainantName,
            String content,
            String answer,
            String etc,
            List<Category> categories,
            User writer,
            FaqStatus status
    ) {
        this.title = title;
        this.complainantName = complainantName;
        this.content = content;
        this.answer = answer;
        this.etc = etc;
        this.writer = writer;
        this.status = status;

        this.faqCategories.clear();

        for (Category category : categories) {
            this.addCategory(category);
        }
    }

    public void delete() {
        this.deletedFlag = true;
        this.deletedAt = LocalDate.now();
    }

    public void addImage(FaqImage image) {
        this.images.add(image);
        image.assignFaq(this);
    }

    public void addFile(FaqFile file) {
        this.files.add(file);
        file.assignFaq(this);
    }
}