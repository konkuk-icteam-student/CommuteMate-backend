package com.better.CommuteMate.attendance.application;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * QR 인증 토큰을 메모리에서 관리하는 매니저
 * - 토큰 생성 및 검증
 * - 만료된 토큰 정리
 */
@Component
public class QrTokenManager {

    // Key: Token(UUID), Value: ExpiryTime
    private final Map<String, LocalDateTime> tokenStore = new ConcurrentHashMap<>();
    
    // 토큰 유효 시간 (초)
    private static final long TOKEN_VALIDITY_SECONDS = 60;

    /**
     * 새로운 QR 인증 토큰 생성
     * @return 생성된 UUID 토큰
     */
    public String generateToken() {
        String token = UUID.randomUUID().toString();
        tokenStore.clear();
        tokenStore.put(token, LocalDateTime.now().plusSeconds(TOKEN_VALIDITY_SECONDS));
        return token;
    }

    /**
     * 토큰 유효성 검증
     * @param token 검증할 토큰
     * @return 유효 여부
     */
    public boolean validateToken(String token) {
        if (token == null || !tokenStore.containsKey(token)) {
            return false;
        }

        LocalDateTime expiry = tokenStore.get(token);
        if (expiry.isBefore(LocalDateTime.now())) {
            tokenStore.remove(token);
            return false;
        }

        return true;
    }
    
    /**
     * 사용된 토큰 폐기 (중복 사용 방지)
     */
    public void invalidateToken(String token) {
        tokenStore.remove(token);
    }
}
