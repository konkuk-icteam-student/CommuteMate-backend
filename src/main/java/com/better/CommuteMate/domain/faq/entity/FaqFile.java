package com.better.CommuteMate.domain.faq.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "faq_file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaqFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String url;

    @Column(name = "s3_key", length = 255, nullable = false)
    private String s3Key;

    @Column(name = "original_name", length = 255, nullable = false)
    private String originalName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faq_id", nullable = false)
    private Faq faq;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static FaqFile create(String url, String s3Key, String originalName, Faq faq) {
        FaqFile file = new FaqFile();
        file.url = url;
        file.s3Key = s3Key;
        file.originalName = originalName;
        file.faq = faq;
        return file;
    }
}