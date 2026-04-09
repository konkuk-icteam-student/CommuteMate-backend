package com.better.CommuteMate.domain.faq.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "faq_history_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaqHistoryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String url;

    @Column(name = "s3_key", length = 255, nullable = false)
    private String s3Key;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faq_history_id", nullable = false)
    private FaqHistory faqHistory;

    public static FaqHistoryImage create(String url, String s3Key, FaqHistory history) {
        FaqHistoryImage image = new FaqHistoryImage();
        image.url = url;
        image.s3Key = s3Key;
        image.faqHistory = history;
        return image;
    }
}