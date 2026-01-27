package com.better.CommuteMate.auth.application;

import com.better.CommuteMate.domain.emailverification.entity.EmailVerificationCode;
import com.better.CommuteMate.domain.emailverification.repository.EmailVerificationCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 만료된 이메일 인증 코드 정리
 *
 * 역할:
 * - 주기적으로 만료된 인증 코드를 DB에서 삭제
 *
 * 실행 주기:
 * - 매일 새벽 3시 (cron: "0 0 3 * * ?")
 * - 또는 1시간마다 (fixedRate: 3600000ms)
 */
@Service
public class EmailVerificationCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationCleanupService.class);

    private final EmailVerificationCodeRepository emailVerificationCodeRepository;

    public EmailVerificationCleanupService(EmailVerificationCodeRepository emailVerificationCodeRepository) {
        this.emailVerificationCodeRepository = emailVerificationCodeRepository;
    }

    /**
     * 만료된 인증 코드 정리 (매일 새벽 3시 실행)
     *
     * cron 표현식: 초 분 시 일 월 요일
     * "0 0 3 * * ?" = 매일 3시 0분 0초
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredCodes() {
        logger.info("만료된 이메일 인증 코드 정리 작업 시작");

        try {
            LocalDateTime now = LocalDateTime.now();
            List<EmailVerificationCode> expiredCodes = emailVerificationCodeRepository.findByExpiresAtBefore(now);

            if (expiredCodes.isEmpty()) {
                logger.info("삭제할 만료된 인증 코드가 없습니다.");
                return;
            }

            int deletedCount = expiredCodes.size();
            emailVerificationCodeRepository.deleteAll(expiredCodes);

            logger.info("만료된 인증 코드 {}개 삭제 완료", deletedCount);

        } catch (Exception e) {
            logger.error("만료된 인증 코드 정리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
