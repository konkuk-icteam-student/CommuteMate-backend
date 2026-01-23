package com.better.CommuteMate.domain.faq.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "faq_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaqHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String title;  // 제목

    @Column(name = "complainant_name", length = 30)
    private String complainantName; //민원인 이름

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;  // 내용

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;  // 답변

    @Column(columnDefinition = "TEXT")
    private String etc;  // 비고

    @ElementCollection
    @CollectionTable(
            name = "faq_history_managers",
            joinColumns = @JoinColumn(name = "faq_history_id")
    )
    @Column(name = "manager_name", length = 30, nullable = false)
    private List<String> managerNames;

    @Column(name = "writer_name", length = 30, nullable = false)
    private String writerName;  // 작성자 이름

    @Column(name = "edited_at", nullable = false)
    private LocalDateTime editedAt;  // 수정된 날짜

    @Column(name = "category_name", length = 100, nullable = false)
    private String categoryName;  // 분류명

    // FK: faq_id → faq(id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faq_id", nullable = false)
    private Faq faq;

    @PrePersist
    protected void onCreate() {
        this.editedAt = LocalDateTime.now();
    }

    @Builder
    private FaqHistory(
            Faq faq,
            String title,
            String complainantName,
            String content,
            String answer,
            String etc,
            String writerName,
            List<String> managerNames,
            String categoryName
    ) {
        this.faq = faq;
        this.title = title;
        this.complainantName = complainantName;
        this.content = content;
        this.answer = answer;
        this.etc = etc;
        this.writerName = writerName;
        this.managerNames = managerNames;
        this.categoryName = categoryName;
        this.editedAt = LocalDateTime.now();
    }

    public static FaqHistory create(Faq faq, String writerName) {
        return FaqHistory.builder()
                .faq(faq)
                .title(faq.getTitle())
                .complainantName(faq.getComplainantName())
                .content(faq.getContent())
                .answer(faq.getAnswer())
                .etc(faq.getEtc())
                .writerName(writerName)
                .managerNames(faq.getCategory().getManagers()
                        .stream()
                        .map(mc -> mc.getManager().getName())
                        .toList())
                .categoryName(faq.getCategory().getName())
                .build();
    }

}