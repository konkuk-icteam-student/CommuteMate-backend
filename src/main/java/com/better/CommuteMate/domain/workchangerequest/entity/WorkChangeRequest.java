package com.better.CommuteMate.domain.workchangerequest.entity;

import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.code.CodeTypeConverter;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "work_change_request", indexes = {
    @Index(name = "idx_wcr_user", columnList = "user_id"),
    @Index(name = "idx_wcr_schedule", columnList = "schedule_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id", nullable = false)
    private Integer requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private WorkSchedule schedule;

    @Convert(converter = CodeTypeConverter.class)
    @Column(name = "type_code", columnDefinition = "CHAR(4)", nullable = false)
    private CodeType typeCode;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Convert(converter = CodeTypeConverter.class)
    @Column(name = "status_code", columnDefinition = "CHAR(4)", nullable = false)
    private CodeType statusCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Integer createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Integer updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}