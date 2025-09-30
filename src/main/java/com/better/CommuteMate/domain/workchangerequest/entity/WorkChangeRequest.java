package com.better.CommuteMate.domain.workchangerequest.entity;

import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "work_change_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkChangeRequest {

    @Id
    @Column(name = "request_id", length = 36, nullable = false)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private WorkSchedule schedule;

    @Column(name = "type_code", columnDefinition = "CHAR(4)", nullable = false)
    private String typeCode;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "status_code", columnDefinition = "CHAR(4)", nullable = false)
    private String statusCode;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;
}