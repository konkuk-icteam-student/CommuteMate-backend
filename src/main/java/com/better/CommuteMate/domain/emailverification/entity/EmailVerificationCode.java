package com.better.CommuteMate.domain.emailverification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Random;

@Entity
@Table(name = "email_verification_code", indexes = {
    @Index(name = "idx_code", columnList = "code"),
    @Index(name = "idx_email", columnList = "email")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    // 6자리 인증번호 (예: "123456")
    @Column(nullable = false, length = 6)
    private String code;

    // 인증할 이메일 (UNIQUE 제약조건 추가)
    @Column(nullable = false, length = 100, unique = true)
    private String email;

    // 코드 만료 시간 (기본 5분)
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // 코드 생성 시간
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 인증 완료 여부
    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    // 인증 실패 횟수
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        // 6자리 랜덤 숫자 생성 (000000 ~ 999999)
        code = String.format("%06d", new Random().nextInt(1000000));
        // 5분 후 만료
        expiresAt = LocalDateTime.now().plusMinutes(5);
    }

    // 이메일 인증 코드 생성
    public static EmailVerificationCode createForEmail(String email) {
        return EmailVerificationCode.builder()
                .email(email)
                .verified(false)
                .build();
    }

    // 코드 만료 여부 확인
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * @param inputCode 사용자가 입력한 코드
     * @return 검증 성공 여부
     */
    public boolean verifyCode(String inputCode) {
        if (isExpired()) {
            return false;
        }

        boolean isValid = this.code.equals(inputCode);

        if (!isValid) {
            this.attemptCount++;
        }

        return isValid;
    }

    /**
     *
     * @return 5회 이상 실패 시 true
     */
    public boolean isMaxAttemptsExceeded() {
        return this.attemptCount >= 5;
    }

    /**
     * 인증 성공 시 verified를 true로 변경
     */
    public void markAsVerified() {
        this.verified = true;
    }
}
