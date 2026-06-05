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

    @Column(name = "storage_path", length = 255, nullable = false)
    private String storagePath;

    @Column(name = "original_name", length = 255, nullable = false)
    private String originalName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faq_id")
    private Faq faq;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static FaqFile create(String url, String storagePath, String originalName) {
        FaqFile file = new FaqFile();
        file.url = url;
        file.storagePath = storagePath;
        file.originalName = originalName;
        return file;
    }

    public void assignFaq(Faq faq) {
        this.faq = faq;
    }

    public void detachFaq() {
        this.faq = null;
    }
}