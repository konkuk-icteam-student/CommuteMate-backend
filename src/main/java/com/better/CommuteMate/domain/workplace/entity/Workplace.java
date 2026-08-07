package com.better.CommuteMate.domain.workplace.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "workplace")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Workplace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workplace_id", nullable = false)
    private Long workplaceId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "allowed_radius_m", nullable = false)
    @Builder.Default
    private Integer allowedRadiusM = 100;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false, length = 36)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    public void updateWorkplace(
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer allowedRadiusM,
            String updatedBy
    ) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.allowedRadiusM = allowedRadiusM;
        this.updatedBy = updatedBy;
    }

    public void activate(String updatedBy) {
        this.isActive = true;
        this.updatedBy = updatedBy;
    }

    public void deactivate(String updatedBy) {
        this.isActive = false;
        this.updatedBy = updatedBy;
    }

    public boolean isWithinAllowedRadius(BigDecimal distanceM) {
        if (distanceM == null || allowedRadiusM == null) {
            return false;
        }

        return distanceM.compareTo(BigDecimal.valueOf(allowedRadiusM)) <= 0;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}