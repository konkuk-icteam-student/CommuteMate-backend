package com.better.CommuteMate.domain.faq.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "faq_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaqImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String url;

    @Column(name = "storage_path", length = 255, nullable = false)
    private String storagePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faq_id")
    private Faq faq;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static FaqImage create(String url, String storagePath) {
        FaqImage image = new FaqImage();
        image.url = url;
        image.storagePath = storagePath;
        return image;
    }

    public void assignFaq(Faq faq) {
        this.faq = faq;
    }

    public void detachFaq() {
        this.faq = null;
    }
}