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
@Table(name = "faq_history_file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaqHistoryFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String url;

    @Column(name = "storage_path", length = 255, nullable = false)
    private String storagePath;

    @Column(name = "original_name", length = 255, nullable = false)
    private String originalName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faq_history_id", nullable = false)
    private FaqHistory faqHistory;

    public static FaqHistoryFile create(
            String url,
            String storagePath,
            String originalName,
            FaqHistory history
    ) {
        FaqHistoryFile file = new FaqHistoryFile();
        file.url = url;
        file.storagePath = storagePath;
        file.originalName = originalName;
        file.faqHistory = history;
        return file;
    }
}