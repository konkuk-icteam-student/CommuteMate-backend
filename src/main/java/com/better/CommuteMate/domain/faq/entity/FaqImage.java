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

    @Column(name = "s3_key", length = 255, nullable = false)
    private String s3Key;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faq_id")
    private Faq faq;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static FaqImage create(String url, String s3Key) {
        FaqImage image = new FaqImage();
        image.url = url;
        image.s3Key = s3Key;
        return image;
    }

    public void assignFaq(Faq faq) {
        this.faq = faq;
    }

    public void detachFaq() {
        this.faq = null;
    }
}