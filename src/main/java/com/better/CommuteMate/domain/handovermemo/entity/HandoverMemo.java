package com.better.CommuteMate.domain.handovermemo.entity;

import com.better.CommuteMate.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "handover_memo", indexes = {
        @Index(name = "idx_handover_memo_org_created", columnList = "organization_id, created_at")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class HandoverMemo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "memo_id", nullable = false)
    private Long memoId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 주의: expiresAt은 KST 보정됨(createdAt 기준 +3일). 향후 서버에서 expiresAt
    // 기반 만료 판정을 추가할 경우, 비교하는 now()도 KST로 맞출 것
    // (전역 타임존이 UTC인 동안 LocalDateTime.now()는 UTC 기준이라 정합 안 맞음).
    @Column(name = "expires_at", nullable = false, updatable = false)
    private LocalDateTime expiresAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private static final long EXPIRE_DAYS = 3;

    // [임시] 전역 타임존(UTC) 미해결로 인한 인수인계 메모 KST 저장 보정용 상수.
    // 전역 타임존을 KST로 전환할 때 이 상수와 아래 Clock 오버로드를 함께 제거할 것
    // (제거하지 않으면 전역 KST + 이 보정 KST가 겹쳐 +18시간 오차가 생김).
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        onCreate(Clock.system(KST_ZONE));
    }

    // [임시] 전역 타임존(UTC) 미해결로 인한 KST 저장 보정. 전역 타임존 KST 전환 시
    // onCreate(Clock)/KST_ZONE 제거하고 onCreate()에서 LocalDateTime.now()를
    // 직접 쓰도록 되돌릴 것. Clock 파라미터는 테스트에서 고정 시각 검증용.
    void onCreate(Clock clock) {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(clock);
        }
        if (expiresAt == null) {
            expiresAt = createdAt.plusDays(EXPIRE_DAYS);
        }
    }
}
