package com.better.CommuteMate.domain.faq.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "faq_relation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaqRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faq_id", nullable = false)
    private Faq faq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_faq_id", nullable = false)
    private Faq relatedFaq;

    public FaqRelation(Faq faq, Faq relatedFaq) {
        this.faq = faq;
        this.relatedFaq = relatedFaq;
    }
}