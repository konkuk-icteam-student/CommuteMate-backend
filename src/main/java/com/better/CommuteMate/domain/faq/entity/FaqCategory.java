package com.better.CommuteMate.domain.faq.entity;

import com.better.CommuteMate.domain.category.entity.Category;
import jakarta.persistence.*;

@Entity
public class FaqCategory {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Faq faq;

    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;
}
