package com.better.CommuteMate.domain.schedule.entity;

import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "work_schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WorkSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", columnDefinition = "CHAR(4)", nullable = false)
    private CodeType statusCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    // 스케줄 제거 요청 보낸 상태. statusCode는 최종적으로 [WS04: 일정 취소] 혹은 [WS01: 신청] 으로 저장됨.
    public void deleteApplySchedule(Long userId, CodeType codeType) {
        this.statusCode = codeType;
        this.updatedBy = userId;
        if(codeType.equals(CodeType.WS02)) {// WS02: 승인(취소가 승인되었다는 뜻) -> WS04: 취소
            this.isDeleted = true;
            this.statusCode = CodeType.WS04;
        }
    }

    // 변경 요청 승인 처리 (관리자용)
    // typeCode로 넘어오는 값 : 요청 유형 코드 (CR01: 수정 요청, CR02: 삭제 요청)
    public void approveChangeRequest(Long adminId, CodeType typeCode) {
        this.updatedBy = adminId;
        if (typeCode.equals(CodeType.CR01)) { // CR01: 수정 요청
            this.statusCode = CodeType.WS02; // WS02: 승인
        } else if (typeCode.equals(CodeType.CR02)) { // CR02: 삭제 요청
            this.statusCode = CodeType.WS04; // WS04: 취소
            this.isDeleted = true;
        }
    }

    // 상태 변경 (관리자용)
    public void updateStatus(CodeType statusCode, Long adminId) {
        this.statusCode = statusCode;
        this.updatedBy = adminId;
    }

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