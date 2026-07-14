package com.better.CommuteMate.domain.workchangerequest.entity;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.global.code.CodeType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * work_change_request_item 테이블 엔티티.
 * change_type_code: CR01=추가(ADD), CR02=삭제(DELETE) — CodeType enum의 EDIT/DELETE 명칭과 별개로,
 * 이 엔티티에서 CR01은 ADD, CR02는 DELETE를 의미한다.
 * schedule_id: ADD 요청 시 기존 일정이 없으므로 nullable.
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

    // CR01=추가(ADD), CR02=삭제(DELETE)
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type_code", columnDefinition = "CHAR(4)", nullable = false)
    private CodeType changeTypeCode;

    // ADD 요청은 아직 schedule이 없으므로 nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = true)
    private WorkSchedule schedule;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
}
