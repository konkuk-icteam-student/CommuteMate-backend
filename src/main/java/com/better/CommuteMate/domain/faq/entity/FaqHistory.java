package com.better.CommuteMate.domain.faq.entity;

import com.better.CommuteMate.domain.faq.embedded.ManagerSnapshot;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "faq_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaqHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30, nullable = false)
    private String title;  // 제목

    @Column(name = "complainant_name", length = 30)
    private String complainantName; //민원인 이름

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;  // 내용

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;  // 답변

    @Column(columnDefinition = "TEXT")
    private String etc;  // 비고

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "faq_history_managers",
            joinColumns = @JoinColumn(name = "faq_history_id")
    )
    private List<ManagerSnapshot> managers;

    @Column(name = "writer_name", length = 30, nullable = false)
    private String writerName;  // 작성자 이름

    @Column(name = "edited_at", nullable = false)
    private LocalDate editedAt;  // 수정된 날짜

    @Column(name = "category_name", length = 100, nullable = false)
    private String categoryName;  // 분류명

    // FK: faq_id → faq(id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faq_id", nullable = false)
    private Faq faq;

    @PrePersist
    protected void onCreate() {
        this.editedAt = LocalDate.now();
    }

    public static FaqHistory create(Faq faq) {
        FaqHistory history = new FaqHistory();
        history.faq = faq;
        history.title = faq.getTitle();
        history.complainantName = faq.getComplainantName();
        history.content = faq.getContent();
        history.answer = faq.getAnswer();
        history.etc = faq.getEtc();
        history.writerName = faq.getWriter().getName();
        history.managers = faq.getCategory().getManagers()
                .stream()
                .map(mc -> new ManagerSnapshot(
                        mc.getManager().getName(),
                        mc.getManager().getTeam().getName(),
                        mc.getCategory().getName()
                ))
                .toList();
        history.categoryName = faq.getCategory().getName();
        return history;
    }

}