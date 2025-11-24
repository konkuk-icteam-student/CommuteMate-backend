package com.better.CommuteMate.domain.emailverification.repository;

import com.better.CommuteMate.domain.emailverification.entity.EmailVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * EmailVerificationCode Repository (OTP 방식)
 *
 * 주요 기능:
 * - 이메일로 인증 코드 조회
 * - 인증 완료된 이메일 확인
 * - 만료된 코드 정리 (배치 작업용)
 */
@Repository
public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {

    Optional<EmailVerificationCode> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * 이메일과 verified 상태로 조회
     * 사용처: 인증 완료된 이메일 확인 (회원가입 시)
     */
    @Query("SELECT e FROM EmailVerificationCode e WHERE e.email = :email AND e.verified = true")
    Optional<EmailVerificationCode> findByEmailAndVerifiedTrue(@Param("email") String email);

    /**
     * 만료된 코드 목록 조회
     * 사용처: 배치 작업으로 만료된 코드 정리
     */
    List<EmailVerificationCode> findByExpiresAtBefore(LocalDateTime dateTime);

    /**
     * 이메일로 코드 삭제
     * 사용처: 재발송 시 기존 코드 삭제, 회원가입 완료 시 삭제
     */
    void deleteByEmail(String email);
}
