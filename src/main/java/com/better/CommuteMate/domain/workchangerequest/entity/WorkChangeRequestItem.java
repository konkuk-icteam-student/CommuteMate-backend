package com.better.CommuteMate.domain.workchangerequest.entity;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.global.code.CodeType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 근로시간 수정 요청의 변경 항목입니다.
 * CR01은 스케줄 추가, CR02는 기존 스케줄 삭제를 의미합니다.
 */
@Entity
@Table(name = "work_change_request_item", indexes = {
        @Index(name = "idx_wcri_request", columnList = "request_id"),
        @Index(name = "idx_wcri_date", columnList = "date")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WorkChangeRequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private WorkChangeRequest request;

    // CR01: 추가, CR02: 삭제
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type_code", columnDefinition = "CHAR(4)", nullable = false)
    private CodeType changeTypeCode;

    // 추가 요청은 아직 생성된 스케줄이 없으므로 nullable입니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = true)
    private WorkSchedule schedule;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
